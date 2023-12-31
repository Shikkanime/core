package fr.shikkanime.services

import com.mortennobel.imagescaling.ResampleOp
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
import kotlin.system.measureTimeMillis

object ImageService {
    data class Image(
        val uuid: String,
        val url: String,
        var bytes: ByteArray = byteArrayOf(),
        var originalSize: Long = 0,
        var size: Long = 0,
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Image) return false

            if (uuid != other.uuid) return false
            if (url != other.url) return false
            if (!bytes.contentEquals(other.bytes)) return false
            if (originalSize != other.originalSize) return false
            if (size != other.size) return false

            return true
        }

        override fun hashCode(): Int {
            var result = uuid.hashCode()
            result = 31 * result + url.hashCode()
            result = 31 * result + bytes.contentHashCode()
            result = 31 * result + originalSize.hashCode()
            result = 31 * result + size.hashCode()
            return result
        }
    }

    private val logger = LoggerFactory.getLogger(javaClass)
    private val threadPool = Executors.newFixedThreadPool(2)
    private val file = File(Constant.dataFolder, "images-cache.shikk")
    private var cache = mutableListOf<Image>()
    private val change = AtomicBoolean(false)

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
        if (!file.exists()) {
            change.set(true)
            saveCache()
            return
        }

        logger.info("Loading images cache...")

        val take = measureTimeMillis {
            val json = String(FileManager.fromGzip(file.readBytes()))
            val map = ObjectParser.fromJson(json, Array<Image>::class.java).toMutableList()
            cache = map
        }

        logger.info("Loaded images cache in $take ms (${cache.size} images)")
    }

    fun saveCache() {
        if (!change.get()) {
            logger.info("No changes detected in images cache")
            return
        }

        if (!file.exists()) {
            file.createNewFile()
        }

        logger.info("Saving images cache...")

        val take = measureTimeMillis {
            file.writeBytes(FileManager.toGzip(ObjectParser.toJson(cache).toByteArray()))
        }

        logger.info(
            "Saved images cache in $take ms (${toHumanReadable(cache.sumOf { it.originalSize })} -> ${
                toHumanReadable(
                    cache.sumOf { it.size })
            })"
        )
        change.set(false)
    }

    fun add(uuid: UUID, url: String, width: Int, height: Int) {
        if (get(uuid) != null || url.isBlank()) {
            return
        }

        val image = Image(uuid.toString(), url)
        cache.add(image)

        threadPool.submit {
            val take = measureTimeMillis {
                try {
                    val httpResponse = runBlocking { HttpRequest().get(url) }
                    val bytes = runBlocking { httpResponse.readBytes() }

                    if (httpResponse.status != HttpStatusCode.OK || bytes.isEmpty()) {
                        logger.warning("Failed to load image $url")
                        remove(uuid)
                        return@measureTimeMillis
                    }

                    val resized = ResampleOp(width, height).filter(ImageIO.read(ByteArrayInputStream(bytes)), null)
                    val tmpFile = File.createTempFile("shikk", ".png").apply {
                        writeBytes(ByteArrayOutputStream().apply { ImageIO.write(resized, "png", this) }.toByteArray())
                    }
                    val webp = FileManager.encodeToWebP(tmpFile.readBytes())
                    tmpFile.delete()

                    if (webp.isEmpty()) {
                        logger.warning("Failed to encode image to WebP")
                        remove(uuid)
                        return@measureTimeMillis
                    }

                    image.bytes = webp
                    image.originalSize = bytes.size.toLong()
                    image.size = webp.size.toLong()
                    cache[cache.indexOf(image)] = image
                    change.set(true)
                } catch (e: Exception) {
                    logger.log(Level.SEVERE, "Failed to load image $url", e)
                    remove(uuid)
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

    fun remove(uuid: UUID) {
        cache.removeIf { it.uuid == uuid.toString() }
    }

    operator fun get(uuid: UUID): Image? = cache.find { it.uuid == uuid.toString() }

    private fun getDominantColor(image: BufferedImage): Color {
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

    private fun Graphics2D.drawAnimeImage(animeImage: BufferedImage, backgroundImage: BufferedImage) {
        drawImage(
            makeRoundedCorner(animeImage, 16),
            (backgroundImage.width - animeImage.width) / 2,
            (backgroundImage.height - animeImage.height) / 2 + 200,
            null
        )
    }

    private fun Graphics2D.drawBannerImage(bannerImage: BufferedImage) {
        drawImage(bannerImage, 25, 15, null)
    }

    private fun Graphics2D.drawText(
        fontTmp: Font,
        episode: EpisodeDto,
        animeImage: BufferedImage,
        backgroundImage: BufferedImage
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

        color = getDominantColor(animeImage)
        font = fontTmp.deriveFont(65f)
        val animeName = episode.anime.shortName
        font = adjustFontSizeToFit(animeName, backgroundImage.width - 200)
        drawString(animeName, (backgroundImage.width - fontMetrics.stringWidth(animeName)) / 2f, 200f)

        color = Color.WHITE
        font = fontTmp.deriveFont(32f)
        font = font.deriveFont(font.style and Font.BOLD.inv())
        val episodeTitle = StringUtils.toEpisodeString(episode)
        font = adjustFontSizeToFit(episodeTitle, backgroundImage.width - 200)
        drawString(episodeTitle, (backgroundImage.width - fontMetrics.stringWidth(episodeTitle)) / 2f, 250f)

        font = fontTmp.deriveFont(32f)
        val nowAvailable = "Maintenant disponible"
        drawString(nowAvailable, (backgroundImage.width - fontMetrics.stringWidth(nowAvailable)) / 2f, 400f)
        val on = "sur ${episode.platform.platformName}"
        drawString(on, (backgroundImage.width - fontMetrics.stringWidth(on)) / 2f, 450f)
    }

    private fun Graphics2D.adjustFontSizeToFit(text: String, maxWidth: Int): Font {
        var fontSizeFit: Boolean
        do {
            font = font.deriveFont(font.size2D - 1)
            fontSizeFit = fontMetrics.stringWidth(text) < maxWidth
        } while (!fontSizeFit)
        return font
    }

    data class Tuple<A, B, C, D>(val a: A, val b: B, val c: C, val d: D)

    private fun loadResources(episode: EpisodeDto): Tuple<BufferedImage, BufferedImage, Font, BufferedImage> {
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
        val bannerImage = ResampleOp(tmpBannerImage.width / bannerScale, tmpBannerImage.height / bannerScale).filter(
            tmpBannerImage,
            null
        )
        val font = Font.createFont(Font.TRUETYPE_FONT, fontFile)
        val animeImage = ResampleOp(480, 720).filter(ImageIO.read(URI(episode.anime.image!!).toURL()), null)
        return Tuple(backgroundImage, bannerImage, font, animeImage)
    }

    fun toEpisodeImage(episode: EpisodeDto): BufferedImage {
        val (backgroundImage, bannerImage, font, animeImage) = loadResources(episode)
        val finalImage = BufferedImage(backgroundImage.width, backgroundImage.height, BufferedImage.TYPE_INT_ARGB)

        val graphics = finalImage.createGraphics().apply {
            setRenderingHints()
            drawBackgroundImage(backgroundImage)
            drawAnimeImage(animeImage, backgroundImage)
            drawBannerImage(bannerImage)
            drawText(font, episode, animeImage, backgroundImage)
        }

        graphics.dispose()
        return finalImage
    }
}