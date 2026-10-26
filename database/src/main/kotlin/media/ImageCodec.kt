package fr.shikkanime.database.media

import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import javax.imageio.ImageIO
import java.lang.foreign.Arena
import java.lang.foreign.FunctionDescriptor
import java.lang.foreign.Linker
import java.lang.foreign.MemorySegment
import java.lang.foreign.SymbolLookup
import java.lang.foreign.ValueLayout.ADDRESS
import java.lang.foreign.ValueLayout.JAVA_BYTE
import java.lang.foreign.ValueLayout.JAVA_FLOAT
import java.lang.foreign.ValueLayout.JAVA_INT
import java.lang.foreign.ValueLayout.JAVA_LONG
import java.lang.invoke.MethodHandle
import kotlin.math.roundToInt

private const val RGBA_CHANNELS = 4
private const val ALPHA_OFFSET = 3
private const val ALPHA_OPAQUE = 0xFF.toByte()
private const val BYTE_MASK = 0xFF
private const val MAX_PIXELS = 40_000_000L
private const val WEBP_HEADER_SIZE = 12
private const val RIFF_MARKER = "RIFF"
private const val WEBP_MARKER = "WEBP"

/**
 * Encodes and decodes WebP through the system `libwebp`, using the Foreign Function & Memory API.
 *
 * The JDK carries no WebP codec: ImageIO only knows BMP, GIF, JPEG, PNG, TIFF and WBMP. Rather than
 * shelling out to `cwebp`, this object calls the C library directly, which encodes and decodes
 * without a process, and works on the musl-based runtime images as long as `libwebp` is installed.
 *
 * The library is looked up in this order: the `webp.library` system property, the `WEBP_LIBRARY`
 * environment variable, then the usual shared-object names.
 *
 * @see ImageCodec.encodeLossy
 * @see ImageCodec.decode
 */
internal object ImageCodec {
    private val lookupArena = Arena.ofShared()
    private val linker = Linker.nativeLinker()
    private val library: SymbolLookup = loadLibrary()

    private val encodeRgba: MethodHandle = linker.downcallHandle(
        symbol("WebPEncodeRGBA"),
        FunctionDescriptor.of(JAVA_LONG, ADDRESS, JAVA_INT, JAVA_INT, JAVA_INT, JAVA_FLOAT, ADDRESS)
    )
    private val encodeLosslessRgba: MethodHandle = linker.downcallHandle(
        symbol("WebPEncodeLosslessRGBA"),
        FunctionDescriptor.of(JAVA_LONG, ADDRESS, JAVA_INT, JAVA_INT, JAVA_INT, ADDRESS)
    )
    private val decodeRgba: MethodHandle = linker.downcallHandle(
        symbol("WebPDecodeRGBA"),
        FunctionDescriptor.of(ADDRESS, ADDRESS, JAVA_LONG, ADDRESS, ADDRESS)
    )
    private val getInfo: MethodHandle = linker.downcallHandle(
        symbol("WebPGetInfo"),
        FunctionDescriptor.of(JAVA_INT, ADDRESS, JAVA_LONG, ADDRESS, ADDRESS)
    )
    private val free: MethodHandle = linker.downcallHandle(
        symbol("WebPFree"),
        FunctionDescriptor.ofVoid(ADDRESS)
    )

    private fun loadLibrary(): SymbolLookup {
        val configured = System.getProperty("webp.library") ?: System.getenv("WEBP_LIBRARY")
        val candidates = listOfNotNull(
            configured,
            "libwebp.so.7",
            "libwebp.so",
            "libwebp.dylib",
            // Homebrew keeps its libraries outside the default loader search path
            "/opt/homebrew/lib/libwebp.dylib",
            "/usr/local/lib/libwebp.dylib",
            "webp"
        )

        for (candidate in candidates) {
            val lookup = runCatching { SymbolLookup.libraryLookup(candidate, lookupArena) }.getOrNull()
            if (lookup != null && lookup.find("WebPEncodeRGBA").isPresent)
                return lookup
        }

        throw IllegalStateException(
            "libwebp is missing: install it, or point the 'webp.library' system property at the library"
        )
    }

    private fun symbol(name: String): MemorySegment =
        library.find(name).orElseThrow { IllegalStateException("libwebp exposes no symbol named '$name'") }

    /**
     * Resizes an image to the dimensions of an image type.
     *
     * A height lower than or equal to zero keeps the source ratio. Transparency is scaled in
     * premultiplied space: scaling a straight-alpha image drags the colour of the transparent
     * pixels into the edges and leaves dark fringes.
     *
     * @param source image to resize.
     * @param width target width in pixels.
     * @param height target height in pixels, or a value lower than or equal to zero to keep the ratio.
     * @return the resized image.
     */
    fun resize(source: BufferedImage, width: Int, height: Int): BufferedImage {
        val targetHeight = if (height > 0)
            height
        else
            (width.toDouble() * source.height / source.width).roundToInt().coerceAtLeast(1)

        val hasAlpha = source.colorModel.hasAlpha()
        var current = source

        // Halving repeatedly reaches the target with far fewer artefacts than one big step
        while (current.width / 2 >= width && current.height / 2 >= targetHeight)
            current = draw(current, current.width / 2, current.height / 2, hasAlpha)

        return draw(current, width, targetHeight, hasAlpha)
    }

    private fun draw(source: BufferedImage, width: Int, height: Int, hasAlpha: Boolean): BufferedImage {
        val target = BufferedImage(
            width,
            height,
            if (hasAlpha) BufferedImage.TYPE_INT_ARGB_PRE else BufferedImage.TYPE_INT_RGB
        )
        val graphics = target.createGraphics()

        try {
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC)
            graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
            graphics.drawImage(source, 0, 0, width, height, null)
        } finally {
            graphics.dispose()
        }

        return target
    }

    /**
     * Encodes an image to lossy WebP.
     *
     * The alpha plane of a lossy WebP is stored without loss: transparency stays exact whatever the
     * quality, only the colour of the fully transparent pixels is approximated.
     *
     * @param image image to encode, transparent or not.
     * @param quality quality factor between 0 and 100.
     * @return the encoded WebP payload.
     * @see ImageCodec.encodeLossless
     */
    fun encodeLossy(image: BufferedImage, quality: Int): ByteArray =
        // A confined arena per call: the native buffers die with the call instead of living for the
        // lifetime of the process
        Arena.ofConfined().use { arena ->
            val input = copyToNative(arena, toRgba(image))
            val output = arena.allocate(ADDRESS)

            val size = encodeRgba.invoke(
                input, image.width, image.height, image.width * RGBA_CHANNELS, quality.toFloat(), output
            ) as Long

            if (size <= 0L)
                throw IllegalStateException("libwebp refused to encode a ${image.width}x${image.height} image")

            readAndFree(output.get(ADDRESS, 0), size)
        }

    /**
     * Encodes an image to lossless WebP.
     *
     * Lossless keeps a logo's edges and transparency pixel-perfect; it costs more bytes, so it is
     * reserved for the artwork types carrying flat transparency rather than photographs.
     *
     * @param image image to encode.
     * @return the encoded WebP payload.
     * @see ImageCodec.encodeLossy
     */
    fun encodeLossless(image: BufferedImage): ByteArray =
        Arena.ofConfined().use { arena ->
            val input = copyToNative(arena, toRgba(image))
            val output = arena.allocate(ADDRESS)

            val size = encodeLosslessRgba.invoke(
                input, image.width, image.height, image.width * RGBA_CHANNELS, output
            ) as Long

            if (size <= 0L)
                throw IllegalStateException("libwebp refused to encode a ${image.width}x${image.height} image")

            readAndFree(output.get(ADDRESS, 0), size)
        }

    /**
     * Decodes an image payload, whatever its container.
     *
     * A WebP payload goes through libwebp; every other container the platforms serve (JPEG, PNG) is
     * handled by ImageIO. The distinction is made on the bytes, never on a declared content type.
     *
     * @param payload image bytes to decode.
     * @return the decoded image, in an ARGB buffer when it carries transparency.
     * @see ImageCodec.decodeWebp
     */
    fun decode(payload: ByteArray): BufferedImage {
        require(payload.isNotEmpty()) { "An empty payload carries no image" }

        if (isWebp(payload))
            return decodeWebp(payload)

        return readWithLimit(payload)
    }

    /**
     * Decodes a payload whose dimensions are read from its header first.
     *
     * ImageIO allocates the full raster on `read`: the size is therefore checked on the header, so a
     * payload declaring an enormous picture is refused without allocating it.
     *
     * @param payload image bytes to decode.
     * @return the decoded image.
     * @throws IllegalArgumentException when the payload carries no readable image, or declares more than [MAX_PIXELS] pixels.
     * @see ImageCodec.decode
     */
    private fun readWithLimit(payload: ByteArray): BufferedImage =
        ImageIO.createImageInputStream(ByteArrayInputStream(payload)).use { stream ->
            val readers = ImageIO.getImageReaders(stream)
            require(readers.hasNext()) { "Unsupported image payload of ${payload.size} bytes" }

            val reader = readers.next()

            try {
                reader.input = stream
                requirePixels(reader.getWidth(0).toLong(), reader.getHeight(0).toLong())
                reader.read(0)
            } finally {
                reader.dispose()
            }
        }

    /**
     * Refuses a picture declaring more pixels than the codec accepts.
     *
     * @param width declared width, in pixels.
     * @param height declared height, in pixels.
     * @throws IllegalArgumentException when the surface exceeds [MAX_PIXELS].
     * @see ImageCodec.decode
     */
    private fun requirePixels(width: Long, height: Long) {
        require(width * height <= MAX_PIXELS) { "Image of ${width}x$height exceeds $MAX_PIXELS pixels" }
    }

    private fun isWebp(payload: ByteArray): Boolean =
        payload.size > WEBP_HEADER_SIZE &&
            String(payload, 0, 4, Charsets.US_ASCII) == RIFF_MARKER &&
            String(payload, 8, 4, Charsets.US_ASCII) == WEBP_MARKER

    /**
     * Decodes a WebP payload through libwebp.
     *
     * Reading goes through the same library as writing, so an artwork stored as WebP comes back
     * without any platform request, transparency included.
     *
     * @param payload WebP bytes to decode.
     * @return the decoded image, in an ARGB buffer when it carries transparency.
     * @see ImageCodec.encodeLossy
     */
    fun decodeWebp(payload: ByteArray): BufferedImage {
        require(payload.isNotEmpty()) { "An empty payload carries no image" }

        return Arena.ofConfined().use { arena ->
            val input = copyToNative(arena, payload)
            val width = arena.allocate(JAVA_INT)
            val height = arena.allocate(JAVA_INT)

            // Dimensions come from the header: an oversized picture is refused before decoding it
            val info = getInfo.invoke(input, payload.size.toLong(), width, height) as Int
            require(info != 0) { "libwebp refused to read a ${payload.size}-byte header" }
            requirePixels(width.get(JAVA_INT, 0).toLong(), height.get(JAVA_INT, 0).toLong())

            val pointer = decodeRgba.invoke(input, payload.size.toLong(), width, height) as MemorySegment

            if (pointer.address() == 0L)
                throw IllegalStateException("libwebp refused to decode a ${payload.size}-byte payload")

            val imageWidth = width.get(JAVA_INT, 0)
            val imageHeight = height.get(JAVA_INT, 0)
            val size = imageWidth.toLong() * imageHeight * RGBA_CHANNELS

            try {
                toImage(pointer.reinterpret(size).toArray(JAVA_BYTE), imageWidth, imageHeight)
            } finally {
                free.invoke(pointer)
            }
        }
    }

    private fun copyToNative(arena: Arena, bytes: ByteArray): MemorySegment {
        val segment = arena.allocate(bytes.size.toLong())
        MemorySegment.copy(bytes, 0, segment, JAVA_BYTE, 0, bytes.size)
        return segment
    }

    private fun readAndFree(pointer: MemorySegment, size: Long): ByteArray {
        val bytes = pointer.reinterpret(size).toArray(JAVA_BYTE)
        free.invoke(pointer)
        return bytes
    }

    /**
     * Packs a `BufferedImage` into the straight RGBA buffer libwebp expects.
     *
     * `getRGB` un-premultiplies the pixels, so an `ARGB_PRE` buffer produced by resizing reaches the
     * encoder as straight alpha instead of being darkened twice.
     */
    private fun toRgba(image: BufferedImage): ByteArray {
        val pixels = IntArray(image.width * image.height)
        image.getRGB(0, 0, image.width, image.height, pixels, 0, image.width)

        val rgba = ByteArray(pixels.size * RGBA_CHANNELS)
        var index = 0

        for (pixel in pixels) {
            rgba[index++] = (pixel shr 16).toByte()
            rgba[index++] = (pixel shr 8).toByte()
            rgba[index++] = pixel.toByte()
            rgba[index++] = (pixel shr 24).toByte()
        }

        return rgba
    }

    private fun toImage(rgba: ByteArray, width: Int, height: Int): BufferedImage {
        val hasAlpha = (rgba.indices step RGBA_CHANNELS).any { rgba[it + ALPHA_OFFSET] != ALPHA_OPAQUE }
        val imageType = if (hasAlpha) BufferedImage.TYPE_INT_ARGB else BufferedImage.TYPE_INT_RGB
        val image = BufferedImage(width, height, imageType)
        val pixels = IntArray(width * height)

        var index = 0
        for (position in pixels.indices) {
            val red = rgba[index++].toInt() and BYTE_MASK
            val green = rgba[index++].toInt() and BYTE_MASK
            val blue = rgba[index++].toInt() and BYTE_MASK
            val alpha = rgba[index++].toInt() and BYTE_MASK

            pixels[position] = (alpha shl 24) or (red shl 16) or (green shl 8) or blue
        }

        image.setRGB(0, 0, width, height, pixels, 0, width)
        return image
    }

}
