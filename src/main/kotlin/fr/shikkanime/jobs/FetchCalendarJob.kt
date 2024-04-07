package fr.shikkanime.jobs

import com.google.inject.Inject
import fr.shikkanime.dtos.CalendarEpisodeDto
import fr.shikkanime.entities.enums.Platform
import fr.shikkanime.services.ImageService
import fr.shikkanime.services.ImageService.drawStringRect
import fr.shikkanime.services.ImageService.setRenderingHints
import fr.shikkanime.services.caches.LanguageCacheService
import fr.shikkanime.utils.*
import fr.shikkanime.utils.StringUtils.capitalizeWords
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.runBlocking
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import java.awt.Color
import java.awt.Font
import java.awt.Graphics2D
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.io.File
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.*
import java.util.logging.Level
import javax.imageio.ImageIO

class FetchCalendarJob : AbstractJob {
    private val logger = LoggerFactory.getLogger(javaClass)

    @Inject
    private lateinit var languageCacheService: LanguageCacheService

    override fun run() {
        runBlocking {
            HttpRequest().use { httpRequest ->
                val response = httpRequest.get("https://anime.icotaku.com/calendrier_diffusion.html")

                if (response.status != HttpStatusCode.OK) {
                    logger.log(Level.SEVERE, "Error: ${response.status}")
                    return@use
                }

                val body = Jsoup.parse(response.bodyAsText())
                val todayElement = requireNotNull(body.select(".calendrier_diffusion")[0]) { "Today not found" }
                val elements = todayElement.select("tr").toMutableList()
                elements.removeAt(0)

                val episodes = getEpisodes(elements, httpRequest)

                val backgroundImage = getBackgroundImage() ?: return@use
                val calendarImage = BufferedImage(backgroundImage.width, 800, BufferedImage.TYPE_INT_ARGB)
                val graphics = calendarImage.createGraphics()
                graphics.setRenderingHints()
                graphics.drawImage(backgroundImage, 0, 0, null)
                graphics.color = Color(0xFFFFFF)
                graphics.font = graphics.font.deriveFont(24f)
                graphics.font = graphics.font.deriveFont(graphics.font.style or Font.BOLD)

                val date = ZonedDateTime.now().format(DateTimeFormatter.ofPattern("EEEE dd MMMM", Locale.FRENCH))

                val dateString = "Les animés du $date".uppercase()
                val dateWidth = graphics.fontMetrics.stringWidth(dateString)
                graphics.drawString(dateString, 25, 60)
                graphics.fillRect(25, 75, dateWidth, 2)
                graphics.drawStringRect("CALENDRIER", 800, 25, 20, 20, backgroundColor = Color(0x2F2F2F))
                val episodesString = drawEpisodes(episodes, graphics, 150, 800)
                graphics.dispose()

                val calendarImageByteArray = try {
                    val byteArrayOutputStream = ByteArrayOutputStream()
                    ImageIO.write(calendarImage, "png", byteArrayOutputStream)
                    byteArrayOutputStream.toByteArray()
                } catch (e: Exception) {
                    logger.log(Level.SEVERE, "Error while converting calendar image for social networks", e)
                    return@use
                }

                val message = "\uD83C\uDF05 Voici les sorties animés du ${date.lowercase()} : \n" +
                        "\n" +
                        "${episodesString.shuffled().take(4).joinToString("\n") { "- $it" }}\n" +
                        "\n" +
                        "Bonne journée à tous !"

                Constant.abstractSocialNetworks.parallelStream().forEach { socialNetwork ->
                    try {
                        socialNetwork.sendCalendar(message, calendarImageByteArray)
                    } catch (e: Exception) {
                        logger.log(
                            Level.SEVERE,
                            "Error while sending calendar for ${
                                socialNetwork.javaClass.simpleName.replace(
                                    "SocialNetwork",
                                    ""
                                )
                            }",
                            e
                        )
                    }
                }
            }
        }
    }

    private fun drawEpisodes(
        episodes: List<CalendarEpisodeDto>,
        graphics: Graphics2D,
        baseY: Int,
        episodeX: Int
    ): MutableList<String> {
        var y = baseY
        val episodesString = mutableListOf<String>()

        episodes.sortedBy { it.platform.lowercase() }.groupBy { it.platform }.forEach { (platformName, episodes) ->
            graphics.color = Color(0xFFFFFF)
            graphics.font = graphics.font.deriveFont(22f)
            val platform = Platform.findByName(platformName) ?: return@forEach

            val platformImage = getPlatformImage(platform)
            platformImage?.let { graphics.drawImage(it, 25, y - 25, null) }
            graphics.drawString(platformName, 65, y)
            graphics.fillRect(
                25,
                y + 15,
                (platformImage?.width ?: 0) + 10 + graphics.fontMetrics.stringWidth(platformName),
                2
            )

            episodes.sortedBy { it.anime.lowercase() }.groupBy { it.anime }.forEach { (anime, episodes) ->
                val twoLines = if (episodes.size == 1) {
                    drawSingleEpisode(episodes.first(), graphics, anime, y, episodeX, episodesString)
                } else {
                    drawMinMaxEpisodes(episodes, graphics, anime, y, episodeX, episodesString)
                }

                y += if (twoLines) 70 else 35
            }

            y += 100
        }
        return episodesString
    }

    private fun drawMinMaxEpisodes(
        episodes: List<CalendarEpisodeDto>,
        graphics: Graphics2D,
        anime: String,
        y: Int,
        episodeX: Int,
        episodesString: MutableList<String>
    ) = episodes.groupBy { it.season }.all { (season, episodes) ->
        val numbers = episodes.mapNotNull { Regex("\\d+").find(it.episode)?.value?.toIntOrNull() }
        val min = numbers.minOrNull() ?: 0
        val max = numbers.maxOrNull() ?: 0

        drawAnimeLine(graphics, anime, season, y).also {
            graphics.drawString("Épisodes $min-$max", episodeX, y + 50)
            episodesString.add("$anime${if (season > 1) " S${season}" else ""} - Épisodes $min-$max")
        }
    }

    private fun drawSingleEpisode(
        episode: CalendarEpisodeDto,
        graphics: Graphics2D,
        anime: String,
        y: Int,
        episodeX: Int,
        episodesString: MutableList<String>
    ) = drawAnimeLine(graphics, anime, episode.season, y).also {
        graphics.drawString(episode.episode, episodeX, y + 50)
        episodesString.add("$anime${if (episode.season > 1) " S${episode.season}" else ""} - ${episode.episode}")
    }

    private fun getPlatformImage(platform: Platform): BufferedImage? {
        val platformImage = runCatching {
            ImageService.makeRoundedCorner(
                ImageIO.read(
                    ClassLoader.getSystemClassLoader().getResourceAsStream("assets/img/platforms/${platform.image}")
                ).resize(32, 32), 360
            )
        }.getOrNull()
        return platformImage
    }

    private fun getBackgroundImage(): BufferedImage? {
        val calendarFolder =
            ClassLoader.getSystemClassLoader()
                .getResource("calendar")?.file?.let { File(it).takeIf { file -> file.exists() } }
                ?: File(
                    Constant.dataFolder,
                    "calendar"
                )
        require(calendarFolder.exists()) { "Calendar folder not found" }
        val backgroundsFolder = File(calendarFolder, "backgrounds")
        require(backgroundsFolder.exists()) { "Background folder not found" }
        require(backgroundsFolder.listFiles()!!.isNotEmpty()) { "Backgrounds not found" }
        val backgroundImage = ImageIO.read(backgroundsFolder.listFiles()!!.random())
        return backgroundImage
    }

    private suspend fun getEpisodes(
        elements: List<Element>,
        httpRequest: HttpRequest
    ) = elements.mapNotNull { element ->
        val animePageElement = element.selectFirst("a") ?: return@mapNotNull null
        val url = animePageElement.attr("href").let { "https://anime.icotaku.com$it" }
        val episode = element.selectFirst(".calendrier_episode")?.text() ?: return@mapNotNull null
        var title = animePageElement.text().trim()
        val season = Regex("Saison (\\d+)").find(title)?.groupValues?.get(1)?.toIntOrNull() ?: 1

        // Get the anime page
        val animePage = httpRequest.get(url)

        if (animePage.status != HttpStatusCode.OK) {
            return@mapNotNull null
        }

        val animeBody = Jsoup.parse(animePage.bodyAsText())
        val list = animeBody.select(".info_fiche > div") ?: return@mapNotNull null
        val licencePlatforms =
            list.find { it.text().contains("Licence VOD") }?.select("a")?.map { it.text() }?.toMutableList()
        licencePlatforms?.removeIf { it.contains("TF1") }

        if (licencePlatforms.isNullOrEmpty()) {
            return@mapNotNull null
        }

        val alternativeTitles =
            list.find { it.text().contains("Titre alternatif") }?.text()?.replace("Titre alternatif :", "")?.split("/")
                ?: emptyList()
        val detectedLanguage = languageCacheService.detectLanguage(title)

        if (detectedLanguage != null && detectedLanguage == "fr" && alternativeTitles.isNotEmpty()) {
            title = alternativeTitles.first().trim()
        }

        title = title.replace(Regex(" - Saison \\d+"), "").trim()

        licencePlatforms.map {
            CalendarEpisodeDto(
                anime = StringUtils.getShortName(title),
                season = season,
                episode = episode.capitalizeWords(),
                platform = it
            )
        }
    }.flatten()

    private fun drawAnimeLine(graphics: Graphics2D, anime: String, season: Int, y: Int): Boolean {
        val s = "${anime}${if (season > 1) " S${season}" else ""}"
        val width = graphics.fontMetrics.stringWidth(s)

        // Split the anime name on two lines if it's too long
        if (width > 750) {
            val words = s.split(" ")
            var separator = words.size / 2
            val (firstHalf, secondHalf) = words.withIndex().partition { (index, _) -> index < separator }
            var first = firstHalf.joinToString(" ") { it.value }
            var second = secondHalf.joinToString(" ") { it.value }

            // If the last word of the first line and the first word of the second line is the same, we move the separator
            // Or if the first word of the first line and the first word of the second line is the same, we move the separator
            if (first.split(" ").last() == second.split(" ").first() || first.split(" ").first() == second.split(" ")
                    .first()
            ) {
                separator++
                val (firstHalfTry, secondHalfTry) = words.withIndex().partition { (index, _) -> index < separator }
                first = firstHalfTry.joinToString(" ") { it.value }
                second = secondHalfTry.joinToString(" ") { it.value }
            }

            graphics.drawString(first, 25, y + 50)
            graphics.drawString(second, 25, y + 80)
            return true
        }

        graphics.drawString(s, 25, y + 50)
        return false
    }
}