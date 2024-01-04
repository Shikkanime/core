package fr.shikkanime.platforms

import com.google.gson.JsonObject
import fr.shikkanime.caches.CountryCodeAnimeIdKeyCache
import fr.shikkanime.entities.Anime
import fr.shikkanime.entities.Episode
import fr.shikkanime.entities.enums.CountryCode
import fr.shikkanime.entities.enums.EpisodeType
import fr.shikkanime.entities.enums.LangType
import fr.shikkanime.entities.enums.Platform
import fr.shikkanime.exceptions.*
import fr.shikkanime.platforms.configuration.CrunchyrollConfiguration
import fr.shikkanime.utils.HttpRequest
import fr.shikkanime.utils.MapCache
import fr.shikkanime.utils.ObjectParser
import fr.shikkanime.utils.ObjectParser.getAsInt
import fr.shikkanime.utils.ObjectParser.getAsLong
import fr.shikkanime.utils.ObjectParser.getAsString
import io.ktor.client.statement.*
import io.ktor.http.*
import java.io.File
import java.time.Duration
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

private const val IMAGE_NULL_ERROR = "Image is null"

class CrunchyrollPlatform : AbstractPlatform<CrunchyrollConfiguration, CountryCode, String>() {
    data class CrunchyrollAnimeContent(
        val image: String,
        val description: String? = null,
    )

    val simulcasts = MapCache<CountryCode, Set<String>>(Duration.ofHours(1)) {
        fun getSimulcastCode(name: String): String {
            val simulcastCodeTmp = name.lowercase().replace(" ", "-")
            val simulcastYear = simulcastCodeTmp.split("-").last()

            val simulcastSeasonCode = when (simulcastCodeTmp.split("-").first()) {
                "printemps" -> "spring"
                "été" -> "summer"
                "automne" -> "fall"
                "hiver" -> "winter"
                else -> throw Exception("Simulcast season not found")
            }

            return "$simulcastSeasonCode-$simulcastYear"
        }

        fun getPreviousSimulcastCode(currentSimulcastCode: String): String {
            return when (currentSimulcastCode.split("-").first()) {
                "spring" -> "winter-${currentSimulcastCode.split("-").last()}"
                "winter" -> "fall-${currentSimulcastCode.split("-").last().toInt() - 1}"
                "fall" -> "summer-${currentSimulcastCode.split("-").last().toInt()}"
                "summer" -> "spring-${currentSimulcastCode.split("-").last().toInt()}"
                else -> throw Exception("Simulcast season not found")
            }
        }

        val httpRequest = HttpRequest()
        val simulcasts = mutableSetOf<String>()

        val simulcastSelector =
            "#content > div > div.app-body-wrapper > div > div > div.erc-browse-collection > div > div:nth-child(1) > div > div > h4 > a"
        val simulcastAnimesSelector = ".erc-browse-cards-collection > .browse-card > div > div > h4 > a"

        try {
            val currentSimulcastContent = httpRequest.getBrowser(
                "https://www.crunchyroll.com/${it.name.lowercase()}/simulcasts",
                simulcastSelector
            )
            val currentSimulcast =
                currentSimulcastContent.select("#content > div > div.app-body-wrapper > div > div > div.header > div > div > span.call-to-action--PEidl.call-to-action--is-m--RVdkI.select-trigger__title-cta--C5-uH.select-trigger__title-cta--is-displayed-on-mobile--6oNk1")
                    .text() ?: return@MapCache simulcasts
            val currentSimulcastCode = getSimulcastCode(currentSimulcast)
            println("Current simulcast code for $it: $currentSimulcast > $currentSimulcastCode")
            val currentSimulcastAnimes =
                currentSimulcastContent.select(simulcastAnimesSelector).map { a -> a.text().lowercase() }.toSet()
            println("Found ${currentSimulcastAnimes.size} animes for the current simulcast")

            val previousSimulcastCode = getPreviousSimulcastCode(currentSimulcastCode)
            println("Previous simulcast code for $it: $previousSimulcastCode")

            val previousSimulcastContent = httpRequest.getBrowser(
                "https://www.crunchyroll.com/${it.name.lowercase()}/simulcasts/seasons/$previousSimulcastCode",
                simulcastSelector
            )
            val previousSimulcastAnimes =
                previousSimulcastContent.select(simulcastAnimesSelector).map { a -> a.text().lowercase() }.toSet()
            println("Found ${previousSimulcastAnimes.size} animes for the previous simulcast")

            val combinedSimulcasts = (currentSimulcastAnimes + previousSimulcastAnimes).toSet()
            simulcasts.addAll(combinedSimulcasts)
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            httpRequest.closeBrowser()
        }

        println(simulcasts)
        return@MapCache simulcasts
    }

    val animeInfoCache = MapCache<CountryCodeAnimeIdKeyCache, CrunchyrollAnimeContent>(Duration.ofDays(1)) {
        val httpRequest = HttpRequest()
        var image: String? = null
        var description: String? = null

        try {
            val content = httpRequest.getBrowser(
                "https://www.crunchyroll.com/${it.countryCode.name.lowercase()}/${it.animeId}",
                "div.undefined:nth-child(1) > figure:nth-child(1) > picture:nth-child(1) > img:nth-child(2)"
            )
            image =
                content.selectXpath("//*[@id=\"content\"]/div/div[2]/div/div[1]/div[2]/div/div/div[2]/div[2]/figure/picture/img")
                    .attr("src")
            description =
                content.selectXpath("//*[@id=\"content\"]/div/div[2]/div/div[2]/div[1]/div[1]/div[5]/div/div/div/p")
                    .text()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            httpRequest.closeBrowser()
        }

        if (image.isNullOrEmpty()) {
            throw Exception("Image is null or empty")
        }

        return@MapCache CrunchyrollAnimeContent(image, description)
    }

    override fun getPlatform(): Platform = Platform.CRUN

    override suspend fun fetchApiContent(key: CountryCode, zonedDateTime: ZonedDateTime): String {
        val url = "https://www.crunchyroll.com/rss/anime?lang=${key.locale?.replace("-", "")}"
        val response = HttpRequest().get(url)

        if (response.status != HttpStatusCode.OK) {
            return ""
        }

        return response.bodyAsText()
    }

    override fun fetchEpisodes(zonedDateTime: ZonedDateTime, bypassFileContent: File?): List<Episode> {
        val list = mutableListOf<Episode>()

        configuration!!.availableCountries.forEach { countryCode ->
            var api =
                if (bypassFileContent != null && bypassFileContent.exists()) bypassFileContent.readText() else getApiContent(
                    countryCode,
                    zonedDateTime
                )
            api = api.replace(System.lineSeparator(), "").replace("\n", "")
            val array = "<item>(.*?)</item>".toRegex().findAll(api).map { it.value }

            array.forEach {
                try {
                    val xml = ObjectParser.fromXml(it, JsonObject::class.java)
                    list.add(convertEpisode(countryCode, xml))
                } catch (_: EpisodeException) {
                    // Ignore
                } catch (_: AnimeException) {
                    // Ignore
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        return list
    }

    override fun reset() {
        TODO("Not yet implemented")
    }

    override fun saveConfiguration() {
        super.saveConfiguration()
        simulcasts.resetWithNewDuration(Duration.ofMinutes(configuration!!.simulcastCheckDelayInMinutes))
    }

    private fun convertEpisode(countryCode: CountryCode, jsonObject: JsonObject): Episode {
        val animeName = jsonObject.getAsString("crunchyroll:seriesTitle") ?: throw Exception("Anime name is null")

        if (configuration!!.blacklistedSimulcasts.contains(animeName.lowercase())) {
            throw AnimeException("\"$animeName\" is blacklisted")
        }

        val restrictions = jsonObject.getAsJsonObject("media:restriction")?.getAsString("")?.split(" ") ?: emptyList()

        if (restrictions.isEmpty() || !restrictions.contains(countryCode.name.lowercase())) {
            throw EpisodeNotAvailableInCountryException("Episode of $animeName is not available in ${countryCode.name}")
        }

        val fullName = jsonObject.getAsString("title") ?: throw Exception("Episode title is null")
        val isDubbed = fullName.contains("(${countryCode.voice})", true)
        val isMovie = fullName.contains("movie", true) || fullName.contains("film", true)

        val subtitles = jsonObject.getAsString("crunchyroll:subtitleLanguages")?.split(",") ?: emptyList()

        if (!isDubbed && (subtitles.isEmpty() || !subtitles.contains(
                countryCode.locale?.replace("-", " - ")?.lowercase()
            ))
        ) {
            throw EpisodeNoSubtitlesOrVoiceException("Episode is not available in ${countryCode.name} with subtitles or voice")
        }

        val langType = if (isDubbed) LangType.VOICE else LangType.SUBTITLES

        val id = jsonObject.getAsString("crunchyroll:mediaId") ?: throw Exception("Id is null")
        val hash = "${countryCode}-${getPlatform()}-$id-$langType"

        if (hashCache.contains(hash)) {
            throw EpisodeAlreadyReleasedException()
        }

        val releaseDate =
            jsonObject.getAsString("pubDate")?.let { ZonedDateTime.parse(it, DateTimeFormatter.RFC_1123_DATE_TIME) }
                ?: throw Exception("Release date is null")

        val season = jsonObject.getAsString("crunchyroll:season")?.toIntOrNull() ?: 1

        val number = jsonObject.getAsString("crunchyroll:episodeNumber")?.toIntOrNull() ?: -1

        val episodeType =
            if (isMovie) EpisodeType.FILM else if (number == -1) EpisodeType.SPECIAL else EpisodeType.EPISODE

        val title = jsonObject.getAsString("crunchyroll:episodeTitle")?.ifBlank { null }

        val url = jsonObject.getAsString("link")?.ifBlank { null } ?: throw Exception("Url is null")

        val images = jsonObject.getAsJsonArray("media:thumbnail") ?: throw Exception(IMAGE_NULL_ERROR)
        val biggestImage =
            images.maxByOrNull { it.asJsonObject.getAsInt("width")!! } ?: throw Exception(IMAGE_NULL_ERROR)
        val image = biggestImage.asJsonObject.getAsString("url")?.ifBlank { null } ?: throw Exception(IMAGE_NULL_ERROR)

        var duration = jsonObject.getAsLong("crunchyroll:duration", -1)

        var isSimulcasted =
            simulcasts[countryCode].contains(animeName.lowercase()) || configuration!!.simulcasts.map { it.name.lowercase() }
                .contains(animeName.lowercase())
        isSimulcasted = isSimulcasted || isMovie

        if (!isSimulcasted) {
            throw AnimeNotSimulcastedException("\"$animeName\" is not simulcasted")
        }

        val splitted = url.split("/")

        if (splitted.size < 2) {
            throw Exception("Anime id is null")
        }

        val animeId = splitted[splitted.size - 2]
        val crunchyrollAnimeContent = animeInfoCache[CountryCodeAnimeIdKeyCache(countryCode, animeId)]
        duration = getWebsiteEpisodeDuration(duration, url)
        hashCache.add(hash)

        return Episode(
            platform = getPlatform(),
            anime = Anime(
                countryCode = countryCode,
                name = animeName,
                releaseDateTime = releaseDate,
                image = crunchyrollAnimeContent.image,
                description = crunchyrollAnimeContent.description
            ),
            episodeType = episodeType,
            langType = langType,
            hash = hash,
            releaseDateTime = releaseDate,
            season = season,
            number = number,
            title = title,
            url = url,
            image = image,
            duration = duration
        )
    }

    private fun getWebsiteEpisodeDuration(defaultDuration: Long, url: String): Long {
        var duration = defaultDuration

        if (duration == -1L) {
            val httpRequest = HttpRequest()

            try {
                val content = httpRequest.getBrowser(
                    url,
                    "#content > div > div.app-body-wrapper > div > div > div.video-player-wrapper > div.erc-watch-premium-upsell"
                )
                val jsonElement = content.select("script[type=\"application/ld+json\"]").first()?.html()

                if (jsonElement.isNullOrBlank()) {
                    return duration
                }

                val durationString =
                    ObjectParser.fromJson(jsonElement, JsonObject::class.java).getAsString("duration")!!
                // Convert duration (ex: PT23M39.96199999999999S) to long seconds
                duration = kotlin.time.Duration.parse(durationString).inWholeSeconds
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                httpRequest.closeBrowser()
            }
        }

        return duration
    }
}