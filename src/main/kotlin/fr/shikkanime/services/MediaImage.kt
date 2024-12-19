package fr.shikkanime.services

import fr.shikkanime.dtos.animes.AnimeDto
import fr.shikkanime.dtos.variants.EpisodeVariantDto
import fr.shikkanime.entities.enums.CountryCode
import fr.shikkanime.utils.Constant
import fr.shikkanime.utils.FileManager
import fr.shikkanime.utils.HttpRequest
import fr.shikkanime.utils.StringUtils
import fr.shikkanime.utils.resize
import io.ktor.client.statement.readRawBytes
import kotlinx.coroutines.runBlocking
import java.awt.AlphaComposite
import java.awt.Color
import java.awt.Font
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.geom.RoundRectangle2D
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.File
import javax.imageio.ImageIO

object MediaImage {
    private data class Dimensions(
        val whiteLinesSize: Int,
        val margin: Int,
        val animeImageWidth: Int,
        val animeImageHeight: Int,
        val episodeInformationHeight: Int,
        val roundedCorner: Int
    )

    fun toMediaImage(episodes: List<EpisodeVariantDto>): BufferedImage {
        val dimensions = Dimensions(
            whiteLinesSize = 25,
            margin = 50,
            animeImageWidth = 480,
            animeImageHeight = 720,
            episodeInformationHeight = 75,
            roundedCorner = 32
        )

        val bannerImage = loadAndResizeBannerImage()
        val font = loadCustomFont()

        val mediaImage = createMediaImage(dimensions, bannerImage)
        val graphics = setupGraphics(mediaImage)

        drawBackground(graphics, mediaImage, dimensions)
        drawAnimeImage(graphics, dimensions, episodes.first().mapping.anime)
        drawEpisodeInformation(graphics, dimensions, font, episodes.first().mapping.anime.countryCode, episodes)
        drawBanner(graphics, mediaImage, bannerImage, dimensions)

        graphics.dispose()
        return mediaImage
    }

    private fun BufferedImage.makeRoundedCorner(cornerRadius: Int): BufferedImage =
        BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB).apply {
            createGraphics().apply {
                setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
                color = Color.WHITE
                fill(
                    RoundRectangle2D.Float(
                        0f,
                        0f,
                        width.toFloat(),
                        height.toFloat(),
                        cornerRadius.toFloat(),
                        cornerRadius.toFloat()
                    )
                )
                composite = AlphaComposite.SrcAtop
                drawImage(this@makeRoundedCorner, 0, 0, null)
            }
        }

    private fun Graphics2D.adjustFontSizeToFit(text: String, maxWidth: Int): Font {
        var size = font.size2D

        while (fontMetrics.stringWidth(text) > maxWidth && size > 12) {
            font = font.deriveFont(--size)
        }

        return font
    }

    private fun loadAndResizeBannerImage(): BufferedImage {
        val mediaImageFolder = File(Constant.dataFolder, "media-image")
        require(mediaImageFolder.exists()) { "The media image folder does not exist" }
        return ImageIO.read(File(mediaImageFolder, "banner.png")).run {
            val scale = height / 75.0
            resize((width / scale).toInt(), (height / scale).toInt())
        }
    }

    private fun loadCustomFont(): Font {
        return FileManager.getInputStreamFromResource("assets/fonts/Satoshi-Regular.ttf").use { inputStream ->
            Font.createFont(Font.TRUETYPE_FONT, inputStream)
        }
    }

    private fun createMediaImage(dimensions: Dimensions, bannerImage: BufferedImage): BufferedImage {
        val width = dimensions.whiteLinesSize + dimensions.margin + dimensions.animeImageWidth + dimensions.margin
        val height = dimensions.whiteLinesSize + dimensions.margin + dimensions.animeImageHeight +
                dimensions.episodeInformationHeight + (dimensions.margin / 2) + bannerImage.height
        return BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
    }

    private fun setupGraphics(mediaImage: BufferedImage): Graphics2D {
        return mediaImage.createGraphics().apply {
            setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR)
            setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)
            setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB)
            setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        }
    }

    private fun drawBackground(graphics: Graphics2D, mediaImage: BufferedImage, dimensions: Dimensions) {
        graphics.color = Color.WHITE
        graphics.fillRect(0, 0, mediaImage.width, mediaImage.height)
        graphics.color = Color.BLACK
        graphics.fillRoundRect(
            dimensions.whiteLinesSize, dimensions.whiteLinesSize, mediaImage.width, mediaImage.height,
            dimensions.roundedCorner, dimensions.roundedCorner
        )
    }

    fun getLongTimeoutImage(url: String): BufferedImage =
        ByteArrayInputStream(runBlocking { HttpRequest().get(url).readRawBytes() }).use { ImageIO.read(it) }

    private fun drawAnimeImage(graphics: Graphics2D, dimensions: Dimensions, anime: AnimeDto) {
        val animeImage = getLongTimeoutImage(anime.image)
            .resize(dimensions.animeImageWidth, dimensions.animeImageHeight)

        graphics.drawImage(
            animeImage.makeRoundedCorner(dimensions.roundedCorner),
            dimensions.whiteLinesSize + dimensions.margin,
            dimensions.whiteLinesSize + dimensions.margin,
            null
        )

        graphics.color = Color(1.0f, 1.0f, 1.0f, 0.05f)
        graphics.fillRoundRect(
            dimensions.whiteLinesSize + dimensions.margin,
            dimensions.whiteLinesSize + dimensions.margin,
            dimensions.animeImageWidth,
            dimensions.animeImageHeight + dimensions.episodeInformationHeight,
            dimensions.roundedCorner,
            dimensions.roundedCorner
        )
    }

    private fun drawEpisodeInformation(
        graphics: Graphics2D,
        dimensions: Dimensions,
        font: Font,
        countryCode: CountryCode,
        episodes: List<EpisodeVariantDto>
    ) {
        graphics.color = Color.WHITE
        graphics.font = font.deriveFont(32f).deriveFont(Font.BOLD)

        val text = buildString {
            append("S${episodes.first().mapping.season} ")
            append(StringUtils.getEpisodeTypePrefixLabel(countryCode, episodes.first().mapping.episodeType))
            append(episodes.first().mapping.number)
            append(" ")
            append(episodes.map { it.audioLocale }.distinct().joinToString(" & ") { StringUtils.toLangTypeString(countryCode, it) })
            append(" | DISPONIBLE")
        }

        graphics.font = graphics.adjustFontSizeToFit(text, dimensions.animeImageWidth - (dimensions.margin / 2) * 2)

        val x =
            dimensions.whiteLinesSize + dimensions.margin + (dimensions.animeImageWidth - graphics.fontMetrics.stringWidth(
                text
            )) / 2
        val y = dimensions.whiteLinesSize + dimensions.margin + dimensions.animeImageHeight +
                (dimensions.episodeInformationHeight + graphics.fontMetrics.height / 2) / 2
        graphics.drawString(text, x, y)
    }

    private fun drawBanner(
        graphics: Graphics2D,
        mediaImage: BufferedImage,
        bannerImage: BufferedImage,
        dimensions: Dimensions
    ) {
        val x = dimensions.whiteLinesSize + (mediaImage.width - dimensions.whiteLinesSize - bannerImage.width) / 2
        val y = mediaImage.height - bannerImage.height
        graphics.drawImage(bannerImage, x, y, null)
    }
}