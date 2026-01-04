package fr.shikkanime.services

import fr.shikkanime.entities.EpisodeVariant
import fr.shikkanime.utils.LoggerFactory
import fr.shikkanime.utils.StringUtils
import java.awt.*
import java.awt.font.TextLayout
import java.awt.geom.AffineTransform
import java.awt.geom.QuadCurve2D
import java.awt.image.BufferedImage
import java.io.File
import java.util.logging.Level
import javax.imageio.ImageIO
import kotlin.math.*
import kotlin.random.Random

object MediaVideo {
    private val logger = LoggerFactory.getLogger(MediaVideo::class.java)

    private const val WIDTH = 768
    private const val HEIGHT = 768
    private const val FPS = 30
    private const val DURATION_SEC = 5
    private const val TOTAL_FRAMES = FPS * DURATION_SEC

    private val COLOR_BG_TOP = Color(0x121212)
    private val COLOR_BG_BOTTOM = Color(0x050505)
    private val COLOR_TEXT_OUTLINE = Color(255, 255, 255, 12)
    private val COLOR_BUTTON = Color(0xfca311)
    private val COLOR_BUTTON_TEXT = Color(0x0a0a0a)
    private val COLOR_SPARKLE = Color(255, 255, 255, 200)
    private const val FONT_SCROLL_SIZE = 120f

    private var cachedOutline: Shape? = null
    private var cachedOutlineMetrics: Pair<Double, Double>? = null
    
    data class Sparkle(
        var x: Double,
        var y: Double,
        var scale: Double,
        var maxScale: Double,
        var opacity: Float,
        var life: Int,
        var maxLife: Int,
        var rotation: Double,
        val offsets: List<Double> = List(6) { Random.nextDouble(-2.0, 2.0) },
        val lengths: List<Double> = List(6) { Random.nextDouble(0.8, 1.2) }
    )

    fun toMediaVideo(vararg variants: EpisodeVariant): File {
        val videoFile = File.createTempFile("shikkanime_video", ".mp4")
        // Suppression de tempDir car on va streamer directement
        
        val assets = MediaImage.getVideoAssets(*variants)
        val sparkles = ArrayList<Sparkle>()
        val baseFont = MediaImage.loadCustomFont()

        ImageIO.setUseCache(false)

        val command = listOf(
            "ffmpeg", "-y",
            "-f", "image2pipe",
            "-vcodec", "mjpeg",
            "-framerate", "$FPS",
            "-i", "-",
            "-c:v", "libx264",
            "-pix_fmt", "yuv420p",
            "-preset", "ultrafast",
            "-crf", "23",
            videoFile.absolutePath
        )

        var process: Process? = null

        try {
            logger.info("Début du streaming vers FFmpeg...")
            process = ProcessBuilder(command)
                .redirectErrorStream(true) // Fusionne stdout et stderr
                .start()
            val ffmpegInput = process.outputStream

            logger.info("Génération et streaming des frames vers FFmpeg...")

            val logReader = Thread {
                process.inputStream.bufferedReader().use { reader ->
                    reader.forEachLine { line -> logger.config("[FFmpeg] $line") }
                }
            }
            logReader.start()

            // On pré-alloue l'image et le contexte graphique une seule fois
            val frame = BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB)
            val g2d = frame.createGraphics()
            setupQualityGraphics(g2d)

            ffmpegInput.use { stream ->
                for (i in 0 until TOTAL_FRAMES) {
                    val gradient = GradientPaint(0f, 0f, COLOR_BG_TOP, WIDTH.toFloat(), HEIGHT.toFloat(), COLOR_BG_BOTTOM)
                    g2d.paint = gradient
                    g2d.fillRect(0, 0, WIDTH, HEIGHT)

                    drawScrollingBackground(g2d, baseFont, i)

                    val logoY = HEIGHT - 100
                    if (assets.logoPlatform != null) {
                        val logoX = (WIDTH - assets.logoPlatform.width) / 2
                        g2d.drawImage(assets.logoPlatform, logoX, logoY, null)
                    }

                    val cardW = assets.poster.width
                    val cardH = assets.poster.height
                    val cardCenterX = WIDTH / 2
                    val cardCenterY = (HEIGHT / 2) - 50

                    val finalPosterX = cardCenterX - (cardW / 2) - 1
                    val finalPosterY = cardCenterY - (cardH / 2)

                    if (i > 15) {
                        val btnProgress = min(1.0, (i - 15) / 10.0)
                        val btnAlpha = btnProgress.toFloat()
                        val btnY = finalPosterY + cardH - 10
                        g2d.composite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, btnAlpha)
                        drawButton(g2d, baseFont, finalPosterX, btnY, cardW, *variants)
                        g2d.composite = AlphaComposite.SrcOver
                    }

                    val scaleAnim = getElasticOutScale(min(1.0, i / 20.0))
                    val at = AffineTransform()
                    at.translate(cardCenterX.toDouble(), cardCenterY.toDouble())
                    at.scale(scaleAnim, scaleAnim)
                    at.translate(-cardW / 2.0, -cardH / 2.0)

                    g2d.drawImage(assets.poster, at, null)

                    updateAndDrawSparkles(g2d, sparkles)

                    if (!process.isAlive) {
                        throw RuntimeException("FFmpeg a quitté avec le code ${process.exitValue()}")
                    }

                    ImageIO.write(frame, "jpg", stream)
                    stream.flush()
                }
            }
            
            g2d.dispose() // On libère les ressources à la fin de la vidéo

            logger.info("Attente de la fin du processus FFmpeg...")

            val exitCode = process.waitFor()
            if (exitCode != 0) {
                throw RuntimeException("FFmpeg a échoué avec le code $exitCode")
            }

            logger.info("Vidéo générée : ${videoFile.length() / 1024} Ko")

        } catch (e: Exception) {
            process?.destroy()
            logger.log(Level.SEVERE, "Erreur génération vidéo", e)
            videoFile.delete()
            throw e
        }

        return videoFile
    }

    private fun setupQualityGraphics(g2d: Graphics2D) {
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)
        g2d.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_QUALITY) // Ajouté
        g2d.setRenderingHint(RenderingHints.KEY_DITHERING, RenderingHints.VALUE_DITHER_ENABLE) // Ajouté
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC)
    }

    private fun drawScrollingBackground(g2d: Graphics2D, font: Font, frameIndex: Int) {
        val text = "NOUVEAU"
        
        if (cachedOutline == null) {
            val derivedFont = font.deriveFont(Font.BOLD, FONT_SCROLL_SIZE)
            val frc = g2d.fontRenderContext
            val textLayout = TextLayout(text, derivedFont, frc)
            val outline = textLayout.getOutline(null)
            val bounds = outline.bounds
            cachedOutline = outline
            cachedOutlineMetrics = (bounds.width + 40.0) to (bounds.height + 20.0)
        }

        val outline = cachedOutline!!
        val (patternWidth, patternHeight) = cachedOutlineMetrics!!
        
        val speed = 3.0
        val shiftX = (frameIndex * speed) % patternWidth
        val shiftY = (frameIndex * speed) % patternHeight

        g2d.color = COLOR_TEXT_OUTLINE
        val oldStroke = g2d.stroke
        g2d.stroke = BasicStroke(2f)

        val oldTrans = g2d.transform
        g2d.rotate(Math.toRadians(-10.0), WIDTH / 2.0, HEIGHT / 2.0)

        // On utilise une seule instance d'AffineTransform pour les translations
        val t = AffineTransform()
        var y = -HEIGHT.toDouble()
        while (y < HEIGHT * 2) {
            var x = -WIDTH.toDouble()
            while (x < WIDTH * 2) {
                t.setToTranslation(x + shiftX, y + shiftY)
                g2d.draw(t.createTransformedShape(outline))
                x += patternWidth
            }
            y += patternHeight
        }

        g2d.transform = oldTrans
        g2d.stroke = oldStroke
    }

    private fun drawButton(g2d: Graphics2D, font: Font, x: Int, y: Int, width: Int, vararg variant: EpisodeVariant) {
        val text = StringUtils.toVariantsString(*variant).uppercase()
        val height = 60

        g2d.color = Color(255, 255, 255, 30)
        g2d.drawRoundRect(x, y, width, height, 20, 20)
        g2d.color = COLOR_BUTTON

        g2d.fillRoundRect(x, y, width, height, 20, 20)
        g2d.fillRect(x, y, width, height / 2)

        g2d.font = font.deriveFont(Font.BOLD, 18f)
        val fm = g2d.fontMetrics
        val textWidth = fm.stringWidth(text)

        val textX = x + (width - textWidth) / 2
        val textY = y + (height - fm.height) / 2 + fm.ascent + 5

        g2d.color = COLOR_BUTTON_TEXT
        g2d.drawString(text, textX, textY)
    }

    private fun updateAndDrawSparkles(g2d: Graphics2D, sparkles: ArrayList<Sparkle>) {
        if (Random.nextFloat() > 0.7) {
            sparkles.add(Sparkle(
                x = Random.nextDouble(50.0, WIDTH - 50.0),
                y = Random.nextDouble(50.0, HEIGHT - 50.0),
                scale = 0.0,
                maxScale = Random.nextDouble(0.5, 1.2),
                opacity = 0.0f,
                life = 0,
                maxLife = Random.nextInt(20, 40),
                rotation = Random.nextDouble(0.0, 90.0)
            ))
        }

        val iterator = sparkles.iterator()
        while (iterator.hasNext()) {
            val s = iterator.next()
            s.life++

            val halfLife = s.maxLife / 2.0
            if (s.life < halfLife) {
                s.scale = s.maxScale * (s.life / halfLife)
                s.opacity = (s.life / halfLife).toFloat()
            } else {
                s.scale = s.maxScale * (1 - (s.life - halfLife) / halfLife)
                s.opacity = (1 - (s.life - halfLife) / halfLife).toFloat()
            }

            s.rotation += 2.0

            if (s.life >= s.maxLife) {
                iterator.remove()
                continue
            }

            g2d.color = Color.WHITE
            g2d.composite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, s.opacity)

            val oldT = g2d.transform
            g2d.translate(s.x, s.y)
            g2d.scale(s.scale, s.scale)
            g2d.rotate(Math.toRadians(s.rotation))

            drawAnimatedStar(g2d, s, 20.0, s.life.toDouble() / s.maxLife)

            g2d.transform = oldT
        }
        g2d.composite = AlphaComposite.SrcOver
    }

    private fun drawAnimatedStar(g2d: Graphics2D, s: Sparkle, size: Double, progress: Double) {
        val numBranches = 6
        val angleStep = 2 * PI / numBranches

        g2d.stroke = BasicStroke(4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)
        g2d.color = COLOR_SPARKLE

        for (i in 0 until numBranches) {
            val branchDelay = i * 0.05
            val branchProgress = ((progress - branchDelay) / 0.5).coerceIn(0.0, 1.0)

            if (branchProgress <= 0) continue

            val startDist = if (progress > 0.6) (progress - 0.6) * 2.0 * size else 0.0
            val endDist = branchProgress * size * s.lengths[i]

            if (endDist > startDist) {
                val angle = i * angleStep + Math.toRadians(s.offsets[i] * 2)

                // Calcul des points de la trajectoire courbe
                val xStart = cos(angle) * startDist
                val yStart = sin(angle) * startDist
                val xEnd = cos(angle) * endDist
                val yEnd = sin(angle) * endDist

                // Le point de contrôle est décalé perpendiculairement pour créer la courbe
                // On utilise les offsets existants pour varier la courbure par branche
                val midDist = (startDist + endDist) / 2.0
                val ctrlAngle = angle + PI / 2.0
                val ctrlX = cos(angle) * midDist + cos(ctrlAngle) * s.offsets[i] * 3
                val ctrlY = sin(angle) * midDist + sin(ctrlAngle) * s.offsets[i] * 3

                // Dessin de la courbe au lieu d'une ligne droite
                val curve = QuadCurve2D.Double(
                    xStart, yStart,
                    ctrlX, ctrlY,
                    xEnd, yEnd
                )
                g2d.draw(curve)
            }
        }
    }

    private fun getElasticOutScale(t: Double): Double {
        if (t == 0.0) return 0.0
        if (t == 1.0) return 1.0
        val p = 0.3
        return 2.0.pow(-10 * t) * sin((t * 10 - 0.75) * (2 * PI) / p) + 1
    }
}