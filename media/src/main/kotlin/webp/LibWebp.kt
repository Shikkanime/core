package fr.shikkanime.media.webp

import java.awt.image.BufferedImage
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

private const val RGBA_CHANNELS = 4
private const val MAX_PIXELS = 40_000_000L
private const val RIFF_MARKER = "RIFF"
private const val WEBP_MARKER = "WEBP"
private const val HEADER_LENGTH = 12
private const val CHUNK_HEADER_LENGTH = 8
private const val VP8X_ALPHA_FLAG = 0x10
private const val VP8L_ALPHA_BIT = 28
private const val LOSSLESS_HEADER_OFFSET = 1
private const val LOSSLESS_HEADER_LENGTH = 5
private const val LIBRARY_PROPERTY = "webp.library"
private const val LIBRARY_ENVIRONMENT = "WEBP_LIBRARY"
private const val MISSING_LIBRARY_MESSAGE =
    "libwebp is missing: install it with `apk add libwebp` (Alpine) or `brew install libwebp` " +
        "(macOS), or point the WEBP_LIBRARY environment variable at the library"

/**
 * Reads and writes WebP payloads through the system `libwebp`, using the Foreign Function and Memory API.
 *
 * The JDK carries no WebP codec, and shelling out to `cwebp` would make the runtime depend on a
 * command. This object calls the C library directly, which keeps the dependency to the shared object
 * itself (`libwebp` on Alpine, Homebrew's `libwebp` on macOS).
 *
 * Native buffers live in an arena confined to the call, so they are released when the call returns
 * whatever happens, and a picture declaring more than [MAX_PIXELS] pixels is refused before anything
 * is allocated.
 *
 * @see WebpImageReaderSpi
 * @see WebpImageWriterSpi
 */
internal object LibWebp {
    private val arena = Arena.ofShared()
    private val linker = Linker.nativeLinker()
    private val entryPoints: EntryPoints? = runCatching { load() }.getOrNull()

    /**
     * Method handles resolved once the library is loaded.
     *
     * @property encodeRgba `WebPEncodeRGBA`, writing a lossy payload.
     * @property encodeLosslessRgba `WebPEncodeLosslessRGBA`, writing a lossless payload.
     * @property decodeRgba `WebPDecodeRGBA`, reading a payload as RGBA.
     * @property getInfo `WebPGetInfo`, reading the dimensions from the header.
     * @property free `WebPFree`, releasing a buffer allocated by the library.
     */
    private class EntryPoints(
        val encodeRgba: MethodHandle,
        val encodeLosslessRgba: MethodHandle,
        val decodeRgba: MethodHandle,
        val getInfo: MethodHandle,
        val free: MethodHandle
    )

    /**
     * Tells whether the native library could be loaded.
     *
     * @return `true` when encoding and decoding are possible on this host.
     */
    fun isAvailable(): Boolean = entryPoints != null

    /**
     * Loads the native library and resolves the entry points used by the codec.
     *
     * The library is looked up through the `webp.library` system property, then the `WEBP_LIBRARY`
     * environment variable, then the usual shared-object names - Homebrew paths included, since it
     * installs outside the loader search path.
     *
     * @return the resolved entry points.
     * @throws WebpCodecException when no library exposing the WebP entry points can be loaded.
     */
    private fun load(): EntryPoints {
        val configured = System.getProperty(LIBRARY_PROPERTY) ?: System.getenv(LIBRARY_ENVIRONMENT)
        val candidates = listOfNotNull(
            configured,
            "libwebp.so.7",
            "libwebp.so",
            "libwebp.dylib",
            "/opt/homebrew/lib/libwebp.dylib",
            "/usr/local/lib/libwebp.dylib",
            "webp"
        )

        val symbols = candidates.firstNotNullOfOrNull { candidate ->
            runCatching { SymbolLookup.libraryLookup(candidate, arena) }
                .getOrNull()
                ?.takeIf { it.find("WebPEncodeRGBA").isPresent }
        } ?: throw WebpCodecException(MISSING_LIBRARY_MESSAGE)

        return EntryPoints(
            encodeRgba = handle(
                symbols,
                "WebPEncodeRGBA",
                FunctionDescriptor.of(JAVA_LONG, ADDRESS, JAVA_INT, JAVA_INT, JAVA_INT, JAVA_FLOAT, ADDRESS)
            ),
            encodeLosslessRgba = handle(
                symbols,
                "WebPEncodeLosslessRGBA",
                FunctionDescriptor.of(JAVA_LONG, ADDRESS, JAVA_INT, JAVA_INT, JAVA_INT, ADDRESS)
            ),
            decodeRgba = handle(
                symbols,
                "WebPDecodeRGBA",
                FunctionDescriptor.of(ADDRESS, ADDRESS, JAVA_LONG, ADDRESS, ADDRESS)
            ),
            getInfo = handle(
                symbols,
                "WebPGetInfo",
                FunctionDescriptor.of(JAVA_INT, ADDRESS, JAVA_LONG, ADDRESS, ADDRESS)
            ),
            free = handle(symbols, "WebPFree", FunctionDescriptor.ofVoid(ADDRESS))
        )
    }

    /**
     * Resolves one C entry point into a method handle.
     *
     * @param symbols library the entry points are read from.
     * @param name name of the C function.
     * @param descriptor signature of the C function.
     * @return the handle calling that function.
     * @throws WebpCodecException when the library does not expose the function.
     */
    private fun handle(symbols: SymbolLookup, name: String, descriptor: FunctionDescriptor): MethodHandle =
        linker.downcallHandle(
            symbols.find(name).orElseThrow { WebpCodecException("libwebp exposes no '$name' entry point") },
            descriptor
        )

    /**
     * Returns the resolved entry points, or refuses the call.
     *
     * @return the entry points of the loaded library.
     * @throws WebpCodecException when the library was not loaded.
     */
    private fun requireEntryPoints(): EntryPoints =
        entryPoints ?: throw WebpCodecException(MISSING_LIBRARY_MESSAGE)

    /**
     * Reads the dimensions and the alpha plane of a payload, without decoding it.
     *
     * @param payload WebP bytes to describe.
     * @return the description carried by the header.
     * @throws WebpCodecException when the payload carries no readable WebP header.
     * @see [WebpInfo]
     */
    fun readInfo(payload: ByteArray): WebpInfo {
        requireWebp(payload)

        return Arena.ofConfined().use { confined ->
            val input = copyToNative(confined, payload)
            val width = confined.allocate(JAVA_INT)
            val height = confined.allocate(JAVA_INT)
            val info = requireEntryPoints().getInfo.invoke(input, payload.size.toLong(), width, height) as Int

            if (info == 0)
                throw WebpCodecException("libwebp refused a ${payload.size}-byte header")

            WebpInfo(width.get(JAVA_INT, 0), height.get(JAVA_INT, 0), carriesAlpha(payload))
        }
    }

    /**
     * Decodes a WebP payload.
     *
     * The picture is built in a three-byte buffer when the payload carries no alpha plane, and in an
     * ARGB buffer otherwise: an opaque artwork must not pay for a channel it does not use.
     *
     * @param payload WebP bytes to decode.
     * @return the decoded picture.
     * @throws WebpCodecException when the payload is not a readable WebP picture, or is too large.
     * @see [readInfo]
     */
    fun decode(payload: ByteArray): BufferedImage {
        val info = readInfo(payload)
        val entryPoints = requireEntryPoints()

        return Arena.ofConfined().use { confined ->
            val input = copyToNative(confined, payload)
            val width = confined.allocate(JAVA_INT)
            val height = confined.allocate(JAVA_INT)
            val pointer = entryPoints.decodeRgba.invoke(
                input, payload.size.toLong(), width, height
            ) as MemorySegment

            if (pointer.address() == 0L)
                throw WebpCodecException("libwebp refused to decode ${payload.size} bytes")

            val imageWidth = width.get(JAVA_INT, 0)
            val imageHeight = height.get(JAVA_INT, 0)
            requirePixels(imageWidth.toLong(), imageHeight.toLong())

            val size = imageWidth.toLong() * imageHeight * RGBA_CHANNELS

            try {
                toImage(pointer.reinterpret(size).toArray(JAVA_BYTE), imageWidth, imageHeight, info.hasAlpha)
            } finally {
                entryPoints.free.invoke(pointer)
            }
        }
    }

    /**
     * Encodes a picture to lossy WebP.
     *
     * The alpha plane is handed to libwebp as-is: the library drops it by itself when every pixel is
     * opaque, which is what the format does natively.
     *
     * @param image picture to encode.
     * @param quality quality factor between 0 and 100.
     * @return the encoded payload.
     * @throws WebpCodecException when the picture is too large, the quality out of range, or libwebp refuses it.
     * @see [encodeLossless]
     */
    fun encode(image: BufferedImage, quality: Int): ByteArray {
        requirePixels(image.width.toLong(), image.height.toLong())
        requireQuality(quality)

        val entryPoints = requireEntryPoints()

        return Arena.ofConfined().use { confined ->
            val input = copyToNative(confined, toRgba(image))
            val output = confined.allocate(ADDRESS)

            val size = entryPoints.encodeRgba.invoke(
                input,
                image.width,
                image.height,
                image.width * RGBA_CHANNELS,
                quality.toFloat(),
                output
            ) as Long

            readAndFree(entryPoints, output, size, image)
        }
    }

    /**
     * Encodes a picture to lossless WebP.
     *
     * @param image picture to encode.
     * @return the encoded payload.
     * @throws WebpCodecException when the picture is too large, or libwebp refuses it.
     * @see [encode]
     */
    fun encodeLossless(image: BufferedImage): ByteArray {
        requirePixels(image.width.toLong(), image.height.toLong())

        val entryPoints = requireEntryPoints()

        return Arena.ofConfined().use { confined ->
            val input = copyToNative(confined, toRgba(image))
            val output = confined.allocate(ADDRESS)

            val size = entryPoints.encodeLosslessRgba.invoke(
                input,
                image.width,
                image.height,
                image.width * RGBA_CHANNELS,
                output
            ) as Long

            readAndFree(entryPoints, output, size, image)
        }
    }

    /**
     * Copies the payload libwebp produced, then releases its buffer.
     *
     * @param entryPoints entry points of the loaded library.
     * @param output segment holding the pointer written by the encoder.
     * @param size size of the payload, as returned by the encoder.
     * @param image picture that was encoded, for the failure message.
     * @return the encoded payload.
     * @throws WebpCodecException when the encoder produced nothing.
     */
    private fun readAndFree(
        entryPoints: EntryPoints,
        output: MemorySegment,
        size: Long,
        image: BufferedImage
    ): ByteArray {
        if (size <= 0L)
            throw WebpCodecException("libwebp refused to encode a ${image.width}x${image.height} image")

        val pointer = output.get(ADDRESS, 0)

        try {
            return pointer.reinterpret(size).toArray(JAVA_BYTE)
        } finally {
            entryPoints.free.invoke(pointer)
        }
    }

    /**
     * Tells whether a payload carries an alpha plane, by reading its container.
     *
     * The container announces it twice depending on the encoding: a `VP8X` flag or an `ALPHA` chunk
     * for a lossy picture, a bit of the lossless header otherwise.
     *
     * @param payload WebP bytes to inspect.
     * @return `true` when the payload carries transparency.
     * @see [readInfo]
     */
    private fun carriesAlpha(payload: ByteArray): Boolean {
        var offset = HEADER_LENGTH

        while (offset + CHUNK_HEADER_LENGTH <= payload.size) {
            val chunk = String(payload, offset, 4, Charsets.US_ASCII)
            val length = readInt(payload, offset + 4)
            val body = offset + CHUNK_HEADER_LENGTH

            when (chunk) {
                "ALPHA" -> return true

                "VP8X" -> if (body < payload.size)
                    return payload[body].toInt() and VP8X_ALPHA_FLAG != 0

                "VP8L" -> if (body + LOSSLESS_HEADER_LENGTH <= payload.size) {
                    // The lossless bitstream starts with a signature byte, then the 32-bit header
                    return readInt(payload, body + LOSSLESS_HEADER_OFFSET) ushr VP8L_ALPHA_BIT and 1 == 1
                }
            }

            offset = body + length + (length and 1)
        }

        return false
    }

    /**
     * Reads a little-endian 32-bit integer.
     *
     * @param payload bytes holding the value.
     * @param offset position of the first byte.
     * @return the value stored at that position.
     * @see [carriesAlpha]
     */
    private fun readInt(payload: ByteArray, offset: Int): Int =
        (payload[offset].toInt() and 0xFF) or
            ((payload[offset + 1].toInt() and 0xFF) shl 8) or
            ((payload[offset + 2].toInt() and 0xFF) shl 16) or
            ((payload[offset + 3].toInt() and 0xFF) shl 24)

    /**
     * Refuses a payload that is not a WebP container.
     *
     * @param payload bytes to inspect.
     * @throws WebpCodecException when the RIFF/WebP header is absent.
     * @see [readInfo]
     */
    private fun requireWebp(payload: ByteArray) {
        if (payload.size <= HEADER_LENGTH ||
            String(payload, 0, 4, Charsets.US_ASCII) != RIFF_MARKER ||
            String(payload, 8, 4, Charsets.US_ASCII) != WEBP_MARKER
        ) {
            throw WebpCodecException("The ${payload.size} bytes given are not a WebP payload")
        }
    }

    /**
     * Refuses a picture declaring more pixels than the codec accepts.
     *
     * @param width declared width, in pixels.
     * @param height declared height, in pixels.
     * @throws WebpCodecException when the surface exceeds [MAX_PIXELS].
     * @see [encode]
     */
    private fun requirePixels(width: Long, height: Long) {
        if (width * height > MAX_PIXELS)
            throw WebpCodecException("Image of ${width}x$height exceeds $MAX_PIXELS pixels")
    }

    /**
     * Refuses a quality factor outside the scale libwebp uses.
     *
     * @param quality quality factor to check.
     * @throws WebpCodecException when the factor is outside 0-100.
     * @see [encode]
     */
    private fun requireQuality(quality: Int) {
        if (quality !in 0..100)
            throw WebpCodecException("Quality $quality is outside the 0-100 range")
    }

    /**
     * Copies bytes into a native segment of the given arena.
     *
     * @param arena arena owning the segment, released with the call.
     * @param bytes bytes to copy.
     * @return the native segment holding the copy.
     * @see [encode]
     */
    private fun copyToNative(arena: Arena, bytes: ByteArray): MemorySegment {
        val segment = arena.allocate(bytes.size.toLong())
        MemorySegment.copy(bytes, 0, segment, JAVA_BYTE, 0, bytes.size)
        return segment
    }

    /**
     * Packs a picture into the straight RGBA buffer libwebp expects.
     *
     * `getRGB` un-premultiplies the pixels, so an `ARGB_PRE` buffer reaches the encoder as straight
     * alpha instead of being darkened twice.
     *
     * @param image picture to pack.
     * @return the RGBA bytes, one pixel after another.
     * @see [encode]
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

    /**
     * Builds a picture from the RGBA buffer libwebp produced.
     *
     * @param rgba decoded bytes, one pixel after another.
     * @param width width of the picture, in pixels.
     * @param height height of the picture, in pixels.
     * @param hasAlpha whether the bytes carry an alpha channel.
     * @return the decoded picture.
     * @see [decode]
     */
    private fun toImage(rgba: ByteArray, width: Int, height: Int, hasAlpha: Boolean): BufferedImage {
        val image = BufferedImage(
            width,
            height,
            if (hasAlpha) BufferedImage.TYPE_INT_ARGB else BufferedImage.TYPE_INT_RGB
        )
        val pixels = IntArray(width * height)
        var index = 0

        for (position in pixels.indices) {
            val red = rgba[index++].toInt() and 0xFF
            val green = rgba[index++].toInt() and 0xFF
            val blue = rgba[index++].toInt() and 0xFF
            val alpha = if (hasAlpha) rgba[index++].toInt() and 0xFF else 0xFF

            pixels[position] = (alpha shl 24) or (red shl 16) or (green shl 8) or blue

            if (!hasAlpha)
                index++
        }

        image.setRGB(0, 0, width, height, pixels, 0, width)
        return image
    }
}
