package fr.shikkanime.services

import fr.shikkanime.entities.Anime
import fr.shikkanime.entities.enums.ImageType
import fr.shikkanime.entities.miscellaneous.GroupedEpisode
import fr.shikkanime.utils.*
import io.ktor.client.statement.*
import kotlinx.coroutines.runBlocking
import java.awt.*
import java.awt.geom.RoundRectangle2D
import java.awt.image.BufferedImage
import java.awt.image.ConvolveOp
import java.awt.image.Kernel
import java.io.ByteArrayInputStream
import java.io.File
import java.time.format.DateTimeFormatter
import java.util.*
import javax.imageio.ImageIO

object MediaImage {
    private const val BLUR_SIZE = 25
    private val blurKernel = FloatArray(BLUR_SIZE * BLUR_SIZE) { 1f / (BLUR_SIZE * BLUR_SIZE) }
    private val httpRequest = HttpRequest(timeout = 5_000L)

    fun toMediaImage(episodes: List<GroupedEpisode>): BufferedImage {
        require(episodes.isNotEmpty()) { "The grouped episodes list is empty" }

        val bannerImage = loadAndResizeBannerImage()
        val font = loadCustomFont()

        if (episodes.size == 1) {
            val groupedEpisode = episodes.first()
            val mediaImage = BufferedImage(768, 768, BufferedImage.TYPE_INT_RGB)
            val graphics = setupGraphics(mediaImage)

            drawBackground(mediaImage, graphics)
            val resizedHeight = drawAnimeImageAndBanner(mediaImage, graphics, groupedEpisode.anime)
            drawEpisodeInformation(mediaImage, graphics, resizedHeight, font, groupedEpisode)
            drawLogo(mediaImage, graphics, bannerImage)
            graphics.dispose()
            return mediaImage
        }

        val columns = 3
        val rows = (episodes.size + columns - 1) / columns
        val cellWidth = 768 / columns
        val cellHeight = 440
        val headerHeight = 80
        val footerHeight = 110
        val mediaImageHeight = (headerHeight + (rows * cellHeight) + footerHeight).coerceAtLeast(768)

        val mediaImage = BufferedImage(768, mediaImageHeight, BufferedImage.TYPE_INT_RGB)
        val graphics = setupGraphics(mediaImage)
        drawBackground(mediaImage, graphics)

        graphics.color = Color.WHITE
        graphics.font = font.deriveFont(16f).deriveFont(Font.BOLD)
        val releaseDateTime = episodes.minOf { it.releaseDateTime }
        val dateLabel = releaseDateTime.format(DateTimeFormatter.ofPattern("EEEE dd MMMM yyyy", Locale.FRENCH)).uppercase()
        val dateLabelWidth = graphics.fontMetrics.stringWidth(dateLabel)
        graphics.drawString(dateLabel, mediaImage.width - dateLabelWidth - 20, 40)

        val totalGridHeight = rows * cellHeight
        val gridYOffset = (mediaImageHeight - headerHeight - footerHeight - totalGridHeight) / 2

        episodes.forEachIndexed { index, groupedEpisode ->
            val row = index / columns
            val col = index % columns
            val itemsInThisRow = if (row < rows - 1) columns else (episodes.size - row * columns)
            val rowWidth = itemsInThisRow * cellWidth
            val rowXOffset = (mediaImage.width - rowWidth) / 2

            val x = rowXOffset + col * cellWidth
            val y = headerHeight + gridYOffset + row * cellHeight

            drawGridEpisode(graphics, x, y, cellWidth, font, groupedEpisode)
        }

        drawLogo(mediaImage, graphics, bannerImage)
        graphics.dispose()
        return mediaImage
    }

    private fun drawLogo(mediaImage: BufferedImage, graphics: Graphics2D, bannerImage: BufferedImage) {
        graphics.drawImage(
            bannerImage,
            (mediaImage.width - bannerImage.width) / 2,
            mediaImage.height - bannerImage.height - 25,
            null
        )
    }

    private fun drawGridEpisode(
        graphics: Graphics2D,
        x: Int,
        y: Int,
        cellWidth: Int,
        font: Font,
        groupedEpisode: GroupedEpisode
    ) {
        val attachmentService = Constant.injector.getInstance(AttachmentService::class.java)
        val thumbnailAttachment = attachmentService.findByEntityUuidTypeAndActive(groupedEpisode.anime.uuid!!, ImageType.THUMBNAIL) ?: return

        val animeImage = getLongTimeoutImage(thumbnailAttachment.url!!).resize(1560, 2340)
        val targetWidth = (cellWidth * 0.95).toInt()
        val targetHeight = (targetWidth * 2340 / 1560)
        val resizedThumbnail = animeImage.resize(targetWidth, targetHeight)

        val thumbnailX = x + (cellWidth - targetWidth) / 2

        graphics.color = Color.WHITE
        graphics.fillRoundRect(
            thumbnailX - 3,
            y - 3,
            targetWidth + 6,
            targetHeight + 6,
            8,
            8
        )

        graphics.drawImage(
            resizedThumbnail.makeRoundedCorner(8),
            thumbnailX,
            y,
            targetWidth,
            targetHeight,
            null
        )

        graphics.color = Color.WHITE
        graphics.font = font.deriveFont(12f).deriveFont(Font.BOLD)
        val variantsLabel = StringUtils.toVariantsString(*groupedEpisode.variants.toTypedArray()).uppercase()
        val labelWidth = graphics.fontMetrics.stringWidth(variantsLabel)

        if (labelWidth > cellWidth - 20) {
            val words = variantsLabel.split(" ")
            var line1 = ""
            var line2 = ""
            var useLine2 = false

            words.forEach { word ->
                if (!useLine2 && graphics.fontMetrics.stringWidth("$line1 $word") < cellWidth - 20) {
                    line1 = if (line1.isEmpty()) word else "$line1 $word"
                } else {
                    useLine2 = true
                    line2 = if (line2.isEmpty()) word else "$line2 $word"
                }
            }

            val l1W = graphics.fontMetrics.stringWidth(line1)
            graphics.drawString(line1, x + (cellWidth - l1W) / 2, y + targetHeight + 25)
            val l2W = graphics.fontMetrics.stringWidth(line2)
            graphics.drawString(line2, x + (cellWidth - l2W) / 2, y + targetHeight + 25 + graphics.fontMetrics.height)
        } else {
            graphics.drawString(variantsLabel, x + (cellWidth - labelWidth) / 2, y + targetHeight + 25)
        }
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

    private fun setupGraphics(mediaImage: BufferedImage): Graphics2D {
        return mediaImage.createGraphics().apply {
            setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR)
            setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)
            setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB)
            setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        }
    }

    private fun drawBackground(mediaImage: BufferedImage, graphics: Graphics2D) {
        val gradientPaint = GradientPaint(0f, mediaImage.height.toFloat(), Color(0x000000), mediaImage.width.toFloat(), 0f, Color(0x1F1F1F))
        graphics.paint = gradientPaint
        graphics.fillRect(0, 0, mediaImage.width, mediaImage.height)
    }

    fun getLongTimeoutImage(url: String): BufferedImage =
        ByteArrayInputStream(runBlocking { HttpRequest.retryOnTimeout(3) { httpRequest.get(url).readRawBytes() } }).use { ImageIO.read(it) }

    private fun drawAnimeImageAndBanner(mediaImage: BufferedImage, graphics: Graphics2D, anime: Anime): Int {
        val attachmentService = Constant.injector.getInstance(AttachmentService::class.java)
        val thumbnailAttachment = attachmentService.findByEntityUuidTypeAndActive(anime.uuid!!, ImageType.THUMBNAIL)
        val bannerAttachment = attachmentService.findByEntityUuidTypeAndActive(anime.uuid, ImageType.BANNER)

        checkNotNull(thumbnailAttachment) { "The anime does not have a thumbnail" }
        checkNotNull(bannerAttachment) { "The anime does not have a banner" }

        val animeBanner = getLongTimeoutImage(bannerAttachment.url!!).resize(1920, 1080)
        val scaleBannerRatio = mediaImage.width.toDouble() / (animeBanner.width + BLUR_SIZE * 2)
        val scaleBannerResize = (animeBanner.height * scaleBannerRatio).toInt()
        val resizedBanner = animeBanner.resize(mediaImage.width, scaleBannerResize)

        val paddedImage = BufferedImage(
            resizedBanner.width + BLUR_SIZE * 2,
            resizedBanner.height + BLUR_SIZE * 2,
            BufferedImage.TYPE_INT_ARGB
        )
        val paddedGraphics = paddedImage.createGraphics()

        paddedGraphics.color = Color(0, 0, 0, 0)
        paddedGraphics.fillRect(0, 0, paddedImage.width, paddedImage.height)
        paddedGraphics.drawImage(resizedBanner, BLUR_SIZE, BLUR_SIZE, null)
        paddedGraphics.dispose()

        val blurredImage = ConvolveOp(Kernel(BLUR_SIZE, BLUR_SIZE, blurKernel), ConvolveOp.EDGE_NO_OP, null)
            .filter(paddedImage, null)

        graphics.drawImage(
            blurredImage,
            -BLUR_SIZE,
            100 - BLUR_SIZE,
            blurredImage.width,
            blurredImage.height,
            null
        )

        val animeImage = getLongTimeoutImage(thumbnailAttachment.url!!).resize(1560, 2340)
        val scaleThumbnailRatio = resizedBanner.height.toDouble() / animeImage.height.toDouble()
        val scaleThumbnailResize = (animeImage.width * scaleThumbnailRatio).toInt()
        val resizedThumbnail = animeImage.resize(scaleThumbnailResize, resizedBanner.height)

        graphics.color = Color.WHITE
        graphics.fillRoundRect(
            ((mediaImage.width - resizedThumbnail.width) / 2) - 5,
            95,
            resizedThumbnail.width + 10,
            resizedThumbnail.height + 10,
            8,
            8
        )

        graphics.drawImage(
            resizedThumbnail.makeRoundedCorner(8),
            (mediaImage.width - resizedThumbnail.width) / 2,
            100,
            resizedThumbnail.width,
            resizedThumbnail.height,
            null
        )

        return resizedThumbnail.height
    }

    private fun drawEpisodeInformation(
        mediaImage: BufferedImage,
        graphics: Graphics2D,
        thumbnailHeight: Int,
        font: Font,
        episode: GroupedEpisode
    ) {
        graphics.color = Color.WHITE
        graphics.font = font.deriveFont(16f).deriveFont(Font.BOLD)
        val releaseDateTime = episode.releaseDateTime
        val dateLabel = releaseDateTime.format(DateTimeFormatter.ofPattern("EEEE dd MMMM yyyy", Locale.FRENCH)).uppercase()
        val dateLabelWidth = graphics.fontMetrics.stringWidth(dateLabel)
        graphics.drawString(
            dateLabel,
            mediaImage.width - dateLabelWidth - 20,
            40
        )

        graphics.font = font.deriveFont(24f).deriveFont(Font.BOLD)
        val label1 = StringUtils.toVariantsString(*episode.variants.toTypedArray()).uppercase()
        val labelWidth = graphics.fontMetrics.stringWidth(label1)
        graphics.drawString(label1, (mediaImage.width - labelWidth) / 2, 100 + thumbnailHeight + 80)
        val label2 = "MAINTENANT DISPONIBLE"
        val label2Width = graphics.fontMetrics.stringWidth(label2)
        graphics.drawString(label2, (mediaImage.width - label2Width) / 2, 100 + thumbnailHeight + 120)
    }
}