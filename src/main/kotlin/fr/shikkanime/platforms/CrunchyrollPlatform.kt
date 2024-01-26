package fr.shikkanime.platforms

import com.google.gson.JsonObject
import com.google.inject.Inject
import fr.shikkanime.caches.CountryCodeAnimeIdKeyCache
import fr.shikkanime.entities.Anime
import fr.shikkanime.entities.Episode
import fr.shikkanime.entities.enums.*
import fr.shikkanime.exceptions.*
import fr.shikkanime.platforms.configuration.CrunchyrollConfiguration
import fr.shikkanime.services.caches.ConfigCacheService
import fr.shikkanime.utils.HttpRequest
import fr.shikkanime.utils.MapCache
import fr.shikkanime.utils.ObjectParser
import fr.shikkanime.utils.ObjectParser.getAsInt
import fr.shikkanime.utils.ObjectParser.getAsLong
import fr.shikkanime.utils.ObjectParser.getAsString
import fr.shikkanime.wrappers.CrunchyrollWrapper
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.runBlocking
import java.io.File
import java.time.Duration
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.logging.Level

private const val IMAGE_NULL_ERROR = "Image is null"

class CrunchyrollPlatform : AbstractPlatform<CrunchyrollConfiguration, CountryCode, List<JsonObject>>() {
    data class CrunchyrollAnimeContent(
        val image: String,
        val banner: String = "",
        val description: String? = null,
    )

    @Inject
    private lateinit var configCacheService: ConfigCacheService

    private val identifiers = MapCache<CountryCode, Pair<String, CrunchyrollWrapper.CMS>>(Duration.ofMinutes(30)) {
        val token = runBlocking { CrunchyrollWrapper.getAnonymousAccessToken() }
        val cms = runBlocking { CrunchyrollWrapper.getCMS(token) }
        return@MapCache token to cms
    }

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

        val simulcasts = mutableSetOf<String>()

        val simulcastSelector =
            "#content > div > div.app-body-wrapper > div > div > div.erc-browse-collection > div > div:nth-child(1) > div > div > h4 > a"
        val simulcastAnimesSelector = ".erc-browse-cards-collection > .browse-card > div > div > h4 > a"

        HttpRequest().use { httpRequest ->
            try {
                val currentSimulcastContent = httpRequest.getBrowser(
                    "https://www.crunchyroll.com/${it.name.lowercase()}/simulcasts",
                    simulcastSelector
                )
                val currentSimulcast =
                    currentSimulcastContent.select("#content > div > div.app-body-wrapper > div > div > div.header > div > div > span.call-to-action--PEidl.call-to-action--is-m--RVdkI.select-trigger__title-cta--C5-uH.select-trigger__title-cta--is-displayed-on-mobile--6oNk1")
                        .text() ?: return@MapCache simulcasts
                val currentSimulcastCode = getSimulcastCode(currentSimulcast)
                logger.info("Current simulcast code for $it: $currentSimulcast > $currentSimulcastCode")
                val currentSimulcastAnimes =
                    currentSimulcastContent.select(simulcastAnimesSelector).map { a -> a.text().lowercase() }.toSet()
                logger.info("Found ${currentSimulcastAnimes.size} animes for the current simulcast")

                val previousSimulcastCode = getPreviousSimulcastCode(currentSimulcastCode)
                logger.info("Previous simulcast code for $it: $previousSimulcastCode")

                val previousSimulcastContent = httpRequest.getBrowser(
                    "https://www.crunchyroll.com/${it.name.lowercase()}/simulcasts/seasons/$previousSimulcastCode",
                    simulcastSelector
                )
                val previousSimulcastAnimes =
                    previousSimulcastContent.select(simulcastAnimesSelector).map { a -> a.text().lowercase() }.toSet()
                logger.info("Found ${previousSimulcastAnimes.size} animes for the previous simulcast")

                val combinedSimulcasts = (currentSimulcastAnimes + previousSimulcastAnimes).toSet()
                simulcasts.addAll(combinedSimulcasts)
            } catch (e: Exception) {
                logger.log(Level.SEVERE, "Error while fetching simulcasts for ${it.name}", e)
            }
        }

        logger.info(simulcasts.joinToString(", "))
        return@MapCache simulcasts
    }

    val animeInfoCache = MapCache<CountryCodeAnimeIdKeyCache, CrunchyrollAnimeContent>(Duration.ofDays(1)) {
        var image: String? = null
        var banner: String? = null
        var description: String? = null

        if (configCacheService.getValueAsBoolean(ConfigPropertyKey.USE_CRUNCHYROLL_API)) {
            val (token, cms) = identifiers[it.countryCode]!!
            val `object` = runBlocking { CrunchyrollWrapper.getObject(token, cms, it.animeId) }[0].asJsonObject
            val postersTall = `object`.getAsJsonObject("images").getAsJsonArray("poster_tall")[0].asJsonArray
            val postersWide = `object`.getAsJsonObject("images").getAsJsonArray("poster_wide")[0].asJsonArray
            image = postersTall?.maxByOrNull { poster -> poster.asJsonObject.getAsInt("width")!! }?.asJsonObject?.getAsString("source")
            banner = postersWide?.maxByOrNull { poster -> poster.asJsonObject.getAsInt("width")!! }?.asJsonObject?.getAsString("source")
            description = `object`.getAsString("description")
        } else {
            HttpRequest().use { httpRequest ->
                try {
                    val content = httpRequest.getBrowser(
                        "https://www.crunchyroll.com/${it.countryCode.name.lowercase()}/${it.animeId}",
                        "div.undefined:nth-child(1) > figure:nth-child(1) > picture:nth-child(1) > img:nth-child(2)"
                    )
                    image =
                        content.selectXpath("//*[@id=\"content\"]/div/div[2]/div/div[1]/div[2]/div/div/div[2]/div[2]/figure/picture/img")
                            .attr("src")
                    banner = content.selectXpath("//*[@id=\"content\"]/div/div[2]/div/div[1]/div[2]/div/div/div[2]/div[1]/figure/picture/img")
                        .attr("src")
                    description =
                        content.selectXpath("//*[@id=\"content\"]/div/div[2]/div/div[2]/div[1]/div[1]/div[5]/div/div/div/p")
                            .text()
                } catch (e: Exception) {
                    logger.log(
                        Level.SEVERE,
                        "Error while fetching anime info for ${it.countryCode.name} - ${it.animeId}",
                        e
                    )
                }
            }
        }

        if (image.isNullOrEmpty()) {
            throw Exception("Image is null or empty")
        }

        if (banner.isNullOrEmpty()) {
            throw Exception("Banner is null or empty")
        }

        return@MapCache CrunchyrollAnimeContent(image!!, banner!!, description)
    }

    override fun getPlatform(): Platform = Platform.CRUN

    private fun jsonObjects(content: String): List<JsonObject> {
        var bodyAsText = content
        bodyAsText = bodyAsText.replace(System.lineSeparator(), "").replace("\n", "")
        return "<item>(.*?)</item>".toRegex().findAll(bodyAsText)
            .map { ObjectParser.fromXml(it.value, JsonObject::class.java) }.toList()
    }

    override suspend fun fetchApiContent(key: CountryCode, zonedDateTime: ZonedDateTime): List<JsonObject> {
        return if (configCacheService.getValueAsBoolean(ConfigPropertyKey.USE_CRUNCHYROLL_API)) {
            CrunchyrollWrapper.getBrowse(identifiers[key]!!.first)
        } else {
            val url = "https://www.crunchyroll.com/rss/anime?lang=${key.locale.replace("-", "")}"
            val response = HttpRequest().get(url)

            if (response.status != HttpStatusCode.OK) {
                emptyList()
            } else {
                jsonObjects(response.bodyAsText())
            }
        }
    }

    private fun parseAPIContent(
        bypassFileContent: File?,
        countryCode: CountryCode,
        zonedDateTime: ZonedDateTime
    ): List<JsonObject> {
        return if (bypassFileContent != null && bypassFileContent.exists()) {
            if (configCacheService.getValueAsBoolean(ConfigPropertyKey.USE_CRUNCHYROLL_API)) {
                ObjectParser.fromJson(bypassFileContent.readText()).getAsJsonArray("items").map { it.asJsonObject }
            } else {
                jsonObjects(bypassFileContent.readText())
            }
        } else getApiContent(
            countryCode,
            zonedDateTime
        )
    }

    override fun fetchEpisodes(zonedDateTime: ZonedDateTime, bypassFileContent: File?): List<Episode> {
        val list = mutableListOf<Episode>()

        configuration!!.availableCountries.forEach { countryCode ->
            val api = parseAPIContent(bypassFileContent, countryCode, zonedDateTime)

            api.forEach {
                try {
                    list.add(
                        if (configCacheService.getValueAsBoolean(ConfigPropertyKey.USE_CRUNCHYROLL_API)) {
                            convertJsonEpisode(countryCode, it)
                        } else {
                            convertXMLEpisode(countryCode, it)
                        }
                    )
                } catch (_: EpisodeException) {
                    // Ignore
                } catch (_: AnimeException) {
                    // Ignore
                } catch (e: Exception) {
                    logger.log(Level.SEVERE, "Error on converting episode", e)
                }
            }
        }

        return list
    }

    override fun saveConfiguration() {
        super.saveConfiguration()
        simulcasts.resetWithNewDuration(Duration.ofMinutes(configuration!!.simulcastCheckDelayInMinutes))
    }

    private fun convertJsonEpisode(countryCode: CountryCode, jsonObject: JsonObject): Episode {
        val episodeMetadata = jsonObject.getAsJsonObject("episode_metadata")
        val animeName = episodeMetadata.getAsString("series_title") ?: throw Exception("Anime name is null")

        if (configuration!!.blacklistedSimulcasts.contains(animeName.lowercase())) {
            throw AnimeException("\"$animeName\" is blacklisted")
        }

        val eligibleRegion = episodeMetadata.getAsString("eligible_region")

        if (eligibleRegion.isNullOrBlank() || !eligibleRegion.contains(countryCode.name)) {
            throw EpisodeNotAvailableInCountryException("Episode of $animeName is not available in ${countryCode.name}")
        }

        val audio = episodeMetadata.getAsString("audio_locale")?.ifBlank { null }
        val isDubbed = audio == countryCode.locale
        val subtitles = episodeMetadata.getAsJsonArray("subtitle_locales").map { it.asString!! }

        if (!isDubbed && (subtitles.isEmpty() || !subtitles.contains(countryCode.locale))) {
            throw EpisodeNoSubtitlesOrVoiceException("Episode is not available in ${countryCode.name} with subtitles or voice")
        }

        val langType = if (isDubbed) LangType.VOICE else LangType.SUBTITLES

        val id = jsonObject.getAsString("external_id")?.split(".")?.last() ?: throw Exception("Id is null")
        val hash = "${countryCode}-${getPlatform()}-$id-$langType"

        if (hashCache.contains(hash)) {
            throw EpisodeAlreadyReleasedException()
        }

        val releaseDate =
            episodeMetadata.getAsString("premium_available_date")?.let { ZonedDateTime.parse(it) }
                ?: throw Exception("Release date is null")

        val season = episodeMetadata.getAsInt("season_number") ?: 1
        val number = episodeMetadata.getAsInt("sequence_number") ?: -1

        val episodeType = if (number == -1) EpisodeType.SPECIAL else EpisodeType.EPISODE
        val title = jsonObject.getAsString("title")?.ifBlank { null }
        val url = "https://www.crunchyroll.com/media-$id"

        val images =
            jsonObject.getAsJsonObject("images").getAsJsonArray("thumbnail")[0].asJsonArray.map { it.asJsonObject }
        val biggestImage =
            images.maxByOrNull { it.asJsonObject.getAsInt("width")!! } ?: throw Exception(IMAGE_NULL_ERROR)
        val image =
            biggestImage.asJsonObject.getAsString("source")?.ifBlank { null } ?: throw Exception(IMAGE_NULL_ERROR)

        var duration = episodeMetadata.getAsLong("duration_ms", -1000) / 1000

        val isSimulcasted =
            simulcasts[countryCode]!!.contains(animeName.lowercase()) || configuration!!.simulcasts.map { it.name.lowercase() }
                .contains(animeName.lowercase())

        if (!isSimulcasted) {
            throw AnimeNotSimulcastedException("\"$animeName\" is not simulcasted")
        }

        val animeId = episodeMetadata.getAsString("series_id") ?: throw Exception("Anime id is null")
        val crunchyrollAnimeContent = animeInfoCache[CountryCodeAnimeIdKeyCache(countryCode, animeId)]!!
        duration = getWebsiteEpisodeDuration(duration, url)
        hashCache.add(hash)

        return Episode(
            platform = getPlatform(),
            anime = Anime(
                countryCode = countryCode,
                name = animeName,
                releaseDateTime = releaseDate,
                image = crunchyrollAnimeContent.image,
                banner = crunchyrollAnimeContent.banner,
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

    private fun convertXMLEpisode(countryCode: CountryCode, jsonObject: JsonObject): Episode {
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
                countryCode.locale.replace("-", " - ").lowercase()
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
            simulcasts[countryCode]!!.contains(animeName.lowercase()) || configuration!!.simulcasts.map { it.name.lowercase() }
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
        val crunchyrollAnimeContent = animeInfoCache[CountryCodeAnimeIdKeyCache(countryCode, animeId)]!!
        duration = getWebsiteEpisodeDuration(duration, url)
        hashCache.add(hash)

        return Episode(
            platform = getPlatform(),
            anime = Anime(
                countryCode = countryCode,
                name = animeName,
                releaseDateTime = releaseDate,
                image = crunchyrollAnimeContent.image,
                banner = crunchyrollAnimeContent.banner,
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
            HttpRequest().use { httpRequest ->
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
                    logger.log(Level.SEVERE, "Error while fetching episode duration", e)
                }
            }
        }

        return duration
    }
}