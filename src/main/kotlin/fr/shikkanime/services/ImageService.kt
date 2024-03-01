package fr.shikkanime.services

import fr.shikkanime.dtos.EpisodeDto
import fr.shikkanime.utils.*
import fr.shikkanime.utils.StringUtils.capitalizeWords
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.runBlocking
import java.awt.*
import java.awt.geom.RoundRectangle2D
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.net.URI
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import java.util.logging.Level
import javax.imageio.ImageIO
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.system.measureTimeMillis

object ImageService {
    enum class Type {
        IMAGE,
        BANNER,
    }

    data class Image(
        val uuid: String,
        val type: Type,
        val url: String,
        var bytes: ByteArray = byteArrayOf(),
        var originalSize: Long = 0,
        var size: Long = 0,
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as Image

            if (uuid != other.uuid) return false
            if (type != other.type) return false
            if (url != other.url) return false
            if (!bytes.contentEquals(other.bytes)) return false
            if (originalSize != other.originalSize) return false
            if (size != other.size) return false

            return true
        }

        override fun hashCode(): Int {
            var result = uuid.hashCode()
            result = 31 * result + type.hashCode()
            result = 31 * result + url.hashCode()
            result = 31 * result + bytes.contentHashCode()
            result = 31 * result + originalSize.hashCode()
            result = 31 * result + size.hashCode()
            return result
        }
    }

    private val logger = LoggerFactory.getLogger(javaClass)
    private val threadPool = Executors.newFixedThreadPool(2)
    private val cache = mutableListOf<Image>()
    private val change = AtomicBoolean(false)
    private const val CACHE_FILE_NUMBER = 5

    private fun toHumanReadable(bytes: Long): String {
        val kiloByte = 1024L
        val megaByte = kiloByte * 1024L
        val gigaByte = megaByte * 1024L
        val teraByte = gigaByte * 1024L

        return when {
            bytes < kiloByte -> "$bytes B"
            bytes < megaByte -> String.format("%.2f KiB", bytes.toDouble() / kiloByte)
            bytes < gigaByte -> String.format("%.2f MiB", bytes.toDouble() / megaByte)
            bytes < teraByte -> String.format("%.2f GiB", bytes.toDouble() / gigaByte)
            else -> String.format("%.2f TiB", bytes.toDouble() / teraByte)
        }
    }

    fun loadCache() {
        logger.info("Loading images cache...")
        val cachePart = mutableListOf<Image>()

        val take = measureTimeMillis {
            (0..<CACHE_FILE_NUMBER).toList().parallelStream().forEach { index ->
                val part = loadCachePart(File(Constant.dataFolder, "images-cache-part-$index.shikk"))
                cachePart.addAll(part)
            }
        }

        this.cache.addAll(cachePart)
        logger.info("Loaded images cache in $take ms (${this.cache.size} images)")
    }

    private fun loadCachePart(file: File): List<Image> {
        if (!file.exists()) {
            return emptyList()
        }

        logger.info("Loading images cache part...")
        val cache = mutableListOf<Image>()

        val take = measureTimeMillis {
            val json = String(FileManager.fromGzip(file.readBytes()))
            val map = ObjectParser.fromJson(json, Array<Image>::class.java).toMutableList()
            cache.addAll(map)
        }

        logger.info("Loaded images cache part in $take ms (${cache.size} images)")
        return cache
    }

    fun saveCache() {
        if (!change.get()) {
            logger.info("No changes detected in images cache")
            return
        }

        change.set(false)

        val cacheSize = cache.size
        val partSize = (cacheSize / CACHE_FILE_NUMBER) + 1
        val parts = cache.toList().chunked(partSize + 1)

        logger.info("Saving images cache...")

        val take = measureTimeMillis {
            parts.parallelStream().forEach { part ->
                val index = parts.indexOf(part)
                saveCachePart(part, File(Constant.dataFolder, "images-cache-part-$index.shikk"))
            }
        }

        logger.info("Saved images cache in $take ms ($originalSize -> $compressedSize)")
    }

    private fun saveCachePart(cache: List<Image>, file: File) {
        if (!file.exists()) {
            file.createNewFile()
        }

        logger.info("Saving images cache part...")
        val take = measureTimeMillis {
            file.writeBytes(FileManager.toGzip(ObjectParser.toJson(cache).toByteArray()))
        }

        logger.info(
            "Saved images cache part in $take ms (${toHumanReadable(cache.sumOf { it.originalSize })} -> ${
                toHumanReadable(
                    cache.sumOf { it.size })
            })"
        )
    }

    fun add(uuid: UUID, type: Type, url: String, width: Int, height: Int) {
        if (get(uuid, type) != null || url.isBlank()) {
            return
        }

        val image = Image(uuid.toString(), type, url)
        cache.add(image)

        threadPool.submit {
            val take = measureTimeMillis {
                try {
                    val httpResponse = runBlocking { HttpRequest().get(url) }
                    val bytes = runBlocking { httpResponse.readBytes() }

                    if (httpResponse.status != HttpStatusCode.OK || bytes.isEmpty()) {
                        logger.warning("Failed to load image $url")
                        remove(uuid, type)
                        return@measureTimeMillis
                    }

                    val resized = ImageIO.read(ByteArrayInputStream(bytes)).resize(width, height)
                    val tmpFile = File.createTempFile("shikk", ".png").apply {
                        writeBytes(ByteArrayOutputStream().apply { ImageIO.write(resized, "png", this) }.toByteArray())
                    }
                    val webp = FileManager.encodeToWebP(tmpFile.readBytes())

                    if (!tmpFile.delete())
                        logger.warning("Can not delete tmp file image")

                    if (webp.isEmpty()) {
                        logger.warning("Failed to encode image to WebP")
                        remove(uuid, type)
                        return@measureTimeMillis
                    }

                    image.bytes = webp
                    image.originalSize = bytes.size.toLong()
                    image.size = webp.size.toLong()
                    cache[cache.indexOf(image)] = image
                    change.set(true)
                } catch (e: Exception) {
                    logger.log(Level.SEVERE, "Failed to load image $url", e)
                    remove(uuid, type)
                }
            }

            logger.info(
                "Encoded image to WebP in ${take}ms (${toHumanReadable(image.originalSize)} -> ${
                    toHumanReadable(
                        image.size
                    )
                })"
            )
        }
    }

    fun remove(uuid: UUID, type: Type) {
        cache.removeIf { it.uuid == uuid.toString() && it.type == type }
    }

    operator fun get(uuid: UUID, type: Type): Image? = cache.find { it.uuid == uuid.toString() && it.type == type }

    val size: Int
        get() = cache.size

    val originalSize: String
        get() = toHumanReadable(cache.sumOf { it.originalSize })

    val compressedSize: String
        get() = toHumanReadable(cache.sumOf { it.size })

    fun invalidate() {
        cache.clear()
        change.set(true)
        addAll()
    }

    fun addAll() {
        val animeService = Constant.injector.getInstance(AnimeService::class.java)
        val episodeService = Constant.injector.getInstance(EpisodeService::class.java)

        animeService.findAllUUIDAndImage().forEach {
            val uuid = it[0] as UUID
            animeService.addImage(uuid, it[1] as String)
            animeService.addBanner(uuid, it[2] as String?)
        }

        episodeService.findAllUUIDAndImage().forEach {
            episodeService.addImage(it[0] as UUID, it[1] as String)
        }
    }

    fun getDominantColor(image: BufferedImage): Color {
        val pixels = IntArray(image.width * image.height).apply {
            image.getRGB(
                0,
                0,
                image.width,
                image.height,
                this,
                0,
                image.width
            )
        }
        val red = IntArray(256)
        val green = IntArray(256)
        val blue = IntArray(256)

        pixels.forEach { rgb ->
            val curRed = rgb shr 16 and 0xFF
            val curGreen = rgb shr 8 and 0xFF
            val curBlue = rgb and 0xFF
            red[curRed]++
            green[curGreen]++
            blue[curBlue]++
        }

        val total = pixels.size
        val redAverage = (0..255).sumOf { i -> i * red[i] } / total.toFloat()
        val greenAverage = (0..255).sumOf { i -> i * green[i] } / total.toFloat()
        val blueAverage = (0..255).sumOf { i -> i * blue[i] } / total.toFloat()

        return Color(redAverage.toInt(), greenAverage.toInt(), blueAverage.toInt())
    }

    private fun makeRoundedCorner(image: BufferedImage, cornerRadius: Int): BufferedImage {
        return BufferedImage(image.width, image.height, BufferedImage.TYPE_INT_ARGB).apply {
            createGraphics().apply {
                composite = AlphaComposite.Src
                setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
                color = Color.WHITE
                fill(
                    RoundRectangle2D.Float(
                        0f,
                        0f,
                        image.width.toFloat(),
                        image.height.toFloat(),
                        cornerRadius.toFloat(),
                        cornerRadius.toFloat()
                    )
                )
                composite = AlphaComposite.SrcAtop
                drawImage(image, 0, 0, null)
                dispose()
            }
        }
    }

    private fun Graphics2D.setRenderingHints() {
        setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR)
        setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)
        setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB)
        setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
    }

    private fun Graphics2D.drawBackgroundImage(backgroundImage: BufferedImage) {
        drawImage(backgroundImage, 0, 0, null)
    }

    private fun Graphics2D.drawAnimeImage(
        episode: EpisodeDto,
        animeImage: BufferedImage,
        backgroundImage: BufferedImage
    ) {
        drawImage(
            makeRoundedCorner(animeImage, 16),
            (backgroundImage.width - animeImage.width) / 2,
            (backgroundImage.height - animeImage.height) / 2 + 125,
            null
        )

        if (episode.uncensored) {
            val width = 100
            val height = 50
            val x = (backgroundImage.width + animeImage.width) / 2 - width / 2
            val y = (backgroundImage.height - animeImage.height) / 2 + 125 - height / 2

            fillRoundRect(
                x,
                y,
                width,
                height,
                16,
                16,
            )

            color = Color.BLACK
            font = font.deriveFont(24f)
            val text = "UNC"
            drawString(text, x + fontMetrics.stringWidth(text) / 2, y + fontMetrics.height / 2 + 20)
        }
    }

    private fun Graphics2D.drawBannerImage(bannerImage: BufferedImage) {
        drawImage(bannerImage, 25, 15, null)
    }

    private fun getRelativeLuminance(color: Color): Double {
        var r = color.red / 255.0
        var g = (color.green / 255.0)
        var b = color.blue / 255.0

        r = if ((r <= 0.03928)) r / 12.92 else ((r + 0.055) / 1.055).pow(2.4)
        g = if ((g <= 0.03928)) g / 12.92 else ((g + 0.055) / 1.055).pow(2.4)
        b = if ((b <= 0.03928)) b / 12.92 else ((b + 0.055) / 1.055).pow(2.4)

        return 0.2126 * r + 0.7152 * g + 0.0722 * b
    }

    private fun adjustColor(color: Color, factor: Double): Color {
        val r = (color.red * factor).coerceIn(0.0, 255.0).toInt()
        val g = (color.green * factor).coerceIn(0.0, 255.0).toInt()
        val b = (color.blue * factor).coerceIn(0.0, 255.0).toInt()
        return Color(r, g, b)
    }

    private fun getAdjustedTextColor(backgroundColor: Color, textColor: Color): Color {
        val backgroundLuminance = getRelativeLuminance(backgroundColor)
        var adjustedTextColor = textColor
        var textLuminance = getRelativeLuminance(adjustedTextColor)

        // Calculate the contrast ratio
        var contrastRatio =
            (max(backgroundLuminance, textLuminance) + 0.05) / (min(backgroundLuminance, textLuminance) + 0.05)

        // Adjust the text color until the contrast ratio is at least 7 (normal text) or 4.5 (large text)
        while (contrastRatio < 4.5) {
            adjustedTextColor = adjustColor(adjustedTextColor, 1.1) // lighten the color by 10%
            textLuminance = getRelativeLuminance(adjustedTextColor)
            contrastRatio =
                (max(backgroundLuminance, textLuminance) + 0.05) / (min(backgroundLuminance, textLuminance) + 0.05)
        }

        return adjustedTextColor
    }

    private fun Graphics2D.drawText(
        fontTmp: Font,
        episode: EpisodeDto,
        animeImage: BufferedImage,
        backgroundImage: BufferedImage,
        platformImage: BufferedImage?,
        adjustColor: Boolean
    ) {
        color = Color.WHITE
        font = fontTmp.deriveFont(24f)
        font = font.deriveFont(font.style and Font.BOLD.inv())
        val currentDateFormatted = LocalDate.now(ZoneId.of("Europe/Paris"))
            .format(DateTimeFormatter.ofPattern("EEEE dd MMMM yyyy", Locale.FRENCH)).capitalizeWords()
        drawString(
            currentDateFormatted,
            (backgroundImage.width - fontMetrics.stringWidth(currentDateFormatted) - 25).toFloat(),
            50f
        )


        val textColor = getDominantColor(animeImage)

        val adjustedTextColor = if (adjustColor) {
            val backgroundColor = getDominantColor(backgroundImage)
            getAdjustedTextColor(backgroundColor, textColor)
        } else {
            textColor
        }

        color = adjustedTextColor
        font = fontTmp.deriveFont(65f)
        val animeName = episode.anime.shortName
        font = adjustFontSizeToFit(animeName, backgroundImage.width - 200)
        drawString(animeName, (backgroundImage.width - fontMetrics.stringWidth(animeName)) / 2f, 150f)
        fillRect(
            ((backgroundImage.width - fontMetrics.stringWidth(animeName)) / 2f).toInt(),
            165,
            fontMetrics.stringWidth(animeName),
            5
        )

        color = Color.WHITE
        font = fontTmp.deriveFont(32f)
        font = font.deriveFont(font.style and Font.BOLD.inv())
        val episodeTitle = StringUtils.toEpisodeString(episode)
        font = adjustFontSizeToFit(episodeTitle, backgroundImage.width - 200)
        val x = (backgroundImage.width - fontMetrics.stringWidth(episodeTitle)) / 2f + ((platformImage?.width
            ?: 0) / 2 + 10)

        if (platformImage != null) {
            drawImage(
                makeRoundedCorner(platformImage, 360),
                (x - platformImage.width - 10).toInt(),
                215 - platformImage.height / 2,
                null
            )
        }

        drawString(episodeTitle, x, 225f)
    }

    private fun Graphics2D.adjustFontSizeToFit(text: String, maxWidth: Int): Font {
        var fontSizeFit: Boolean
        do {
            font = font.deriveFont(font.size2D - 1)
            fontSizeFit = fontMetrics.stringWidth(text) < maxWidth
        } while (!fontSizeFit)
        return font
    }

    data class Tuple<A, B, C, D, E>(val a: A, val b: B, val c: C, val d: D, val e: E)

    private fun loadResources(episode: EpisodeDto): Tuple<BufferedImage, BufferedImage, Font, BufferedImage, BufferedImage?> {
        val mediaImageFolder = File(Constant.dataFolder, "media-image")
        require(mediaImageFolder.exists()) { "Media image folder not found" }
        val backgroundsFolder = File(mediaImageFolder, "backgrounds")
        require(backgroundsFolder.exists()) { "Background folder not found" }
        require(backgroundsFolder.listFiles()!!.isNotEmpty()) { "Backgrounds not found" }
        val bannerFile = File(mediaImageFolder, "banner.png")
        require(bannerFile.exists()) { "Banner image not found" }
        val fontFile = File(mediaImageFolder, "font.ttf")
        require(fontFile.exists()) { "Font not found" }

        val backgroundImage = ImageIO.read(backgroundsFolder.listFiles()!!.random())
        val tmpBannerImage = ImageIO.read(bannerFile)
        val bannerScale = 3
        val bannerImage = tmpBannerImage.resize(tmpBannerImage.width / bannerScale, tmpBannerImage.height / bannerScale)
        val font = Font.createFont(Font.TRUETYPE_FONT, fontFile)
        val scale = 1.0
        val animeImage =
            ImageIO.read(URI(episode.anime.image!!).toURL()).resize((480 / scale).toInt(), (720 / scale).toInt())

        val platformImage = try {
            ImageIO.read(URI("http://localhost:37100/assets/img/platforms/${episode.platform.image}").toURL())
                .resize(32, 32)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }

        return Tuple(backgroundImage, bannerImage, font, animeImage, platformImage)
    }

    fun toEpisodeImage(episode: EpisodeDto, adjustColor: Boolean = true): BufferedImage {
        val (backgroundImage, bannerImage, font, animeImage, platformImage) = loadResources(episode)
        val finalImage = BufferedImage(backgroundImage.width, backgroundImage.height, BufferedImage.TYPE_INT_ARGB)

        val graphics = finalImage.createGraphics().apply {
            setRenderingHints()
            drawBackgroundImage(backgroundImage)
            drawAnimeImage(episode, animeImage, backgroundImage)
            drawBannerImage(bannerImage)
            drawText(font, episode, animeImage, backgroundImage, platformImage, adjustColor)
        }

        graphics.dispose()
        return finalImage
    }
}