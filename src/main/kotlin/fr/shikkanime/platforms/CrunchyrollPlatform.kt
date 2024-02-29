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
import fr.shikkanime.utils.*
import fr.shikkanime.utils.ObjectParser.getAsBoolean
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

class CrunchyrollPlatform : AbstractPlatform<CrunchyrollConfiguration, CountryCode, List<JsonObject>>() {
    data class CrunchyrollAnimeContent(
        val image: String,
        val banner: String = "",
        val description: String? = null,
        val simulcast: Boolean = false,
    )

    @Inject
    private lateinit var configCacheService: ConfigCacheService

    private val identifiers = MapCache<CountryCode, Pair<String, CrunchyrollWrapper.CMS>>(Duration.ofMinutes(30)) {
        val token = runBlocking { CrunchyrollWrapper.getAnonymousAccessToken() }
        val cms = runBlocking { CrunchyrollWrapper.getCMS(token) }
        return@MapCache token to cms
    }

    val simulcasts = MapCache<CountryCode, Set<String>>(Duration.ofHours(1)) {
        val simulcastSeries = mutableSetOf<String>()

        if (configCacheService.getValueAsBoolean(ConfigPropertyKey.USE_CRUNCHYROLL_API)) {
            val accessToken = identifiers[it]!!.first
            val simulcasts = runBlocking { CrunchyrollWrapper.getSimulcasts(it.locale, accessToken) }.take(2)
                .map { simulcast -> simulcast.getAsString("id") }

            val series = simulcasts.flatMap { simulcastId ->
                runBlocking {
                    CrunchyrollWrapper.getBrowse(
                        it.locale,
                        accessToken,
                        sortBy = CrunchyrollWrapper.SortType.POPULARITY,
                        type = CrunchyrollWrapper.MediaType.SERIES,
                        100,
                        simulcast = simulcastId
                    )
                }
            }.map { jsonObject -> jsonObject.getAsString("title")!!.lowercase() }.toSet()

            simulcastSeries.addAll(series)
        } else {
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
                            .text() ?: return@MapCache simulcastSeries
                    val currentSimulcastCode = getSimulcastCode(currentSimulcast)
                    logger.info("Current simulcast code for $it: $currentSimulcast > $currentSimulcastCode")
                    val currentSimulcastAnimes =
                        currentSimulcastContent.select(simulcastAnimesSelector).map { a -> a.text().lowercase() }
                            .toSet()
                    logger.info("Found ${currentSimulcastAnimes.size} animes for the current simulcast")

                    val previousSimulcastCode = getPreviousSimulcastCode(currentSimulcastCode)
                    logger.info("Previous simulcast code for $it: $previousSimulcastCode")

                    val previousSimulcastContent = httpRequest.getBrowser(
                        "https://www.crunchyroll.com/${it.name.lowercase()}/simulcasts/seasons/$previousSimulcastCode",
                        simulcastSelector
                    )
                    val previousSimulcastAnimes =
                        previousSimulcastContent.select(simulcastAnimesSelector).map { a -> a.text().lowercase() }
                            .toSet()
                    logger.info("Found ${previousSimulcastAnimes.size} animes for the previous simulcast")

                    val combinedSimulcasts = (currentSimulcastAnimes + previousSimulcastAnimes).toSet()
                    simulcastSeries.addAll(combinedSimulcasts)
                } catch (e: Exception) {
                    logger.log(Level.SEVERE, "Error while fetching simulcasts for ${it.name}", e)
                }
            }
        }

        logger.info(simulcastSeries.joinToString(", "))
        return@MapCache simulcastSeries
    }

    val animeInfoCache = MapCache<CountryCodeAnimeIdKeyCache, CrunchyrollAnimeContent>(Duration.ofDays(1)) {
        var image: String? = null
        var banner: String? = null
        var description: String? = null
        var simulcast = false

        if (configCacheService.getValueAsBoolean(ConfigPropertyKey.USE_CRUNCHYROLL_API)) {
            val (token, cms) = identifiers[it.countryCode]!!
            val `object` = runBlocking {
                CrunchyrollWrapper.getObject(
                    it.countryCode.locale,
                    token,
                    cms,
                    it.animeId
                )
            }[0]
            val postersTall = `object`.getAsJsonObject("images").getAsJsonArray("poster_tall")[0].asJsonArray
            val postersWide = `object`.getAsJsonObject("images").getAsJsonArray("poster_wide")[0].asJsonArray
            image =
                postersTall?.maxByOrNull { poster -> poster.asJsonObject.getAsInt("width")!! }?.asJsonObject?.getAsString(
                    "source"
                )
            banner =
                postersWide?.maxByOrNull { poster -> poster.asJsonObject.getAsInt("width")!! }?.asJsonObject?.getAsString(
                    "source"
                )
            description = `object`.getAsString("description")
            simulcast = `object`.getAsJsonObject("series_metadata").getAsBoolean("is_simulcast") ?: false
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
                    banner =
                        content.selectXpath("//*[@id=\"content\"]/div/div[2]/div/div[1]/div[2]/div/div/div[2]/div[1]/figure/picture/img")
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

        return@MapCache CrunchyrollAnimeContent(image!!, banner!!, description, simulcast)
    }

    private fun getSimulcastCode(name: String): String {
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

    private fun getPreviousSimulcastCode(currentSimulcastCode: String): String {
        return when (currentSimulcastCode.split("-").first()) {
            "spring" -> "winter-${currentSimulcastCode.split("-").last()}"
            "winter" -> "fall-${currentSimulcastCode.split("-").last().toInt() - 1}"
            "fall" -> "summer-${currentSimulcastCode.split("-").last().toInt()}"
            "summer" -> "spring-${currentSimulcastCode.split("-").last().toInt()}"
            else -> throw Exception("Simulcast season not found")
        }
    }

    override fun getPlatform(): Platform = Platform.CRUN

    override fun getConfigurationClass() = CrunchyrollConfiguration::class.java

    private fun jsonObjects(content: String): List<JsonObject> {
        var bodyAsText = content
        bodyAsText = bodyAsText.replace(System.lineSeparator(), "").replace("\n", "")
        return "<item>(.*?)</item>".toRegex().findAll(bodyAsText)
            .map { ObjectParser.fromXml(it.value, JsonObject::class.java) }.toList()
    }

    override suspend fun fetchApiContent(key: CountryCode, zonedDateTime: ZonedDateTime): List<JsonObject> {
        return if (configCacheService.getValueAsBoolean(ConfigPropertyKey.USE_CRUNCHYROLL_API)) {
            CrunchyrollWrapper.getBrowse(key.locale, identifiers[key]!!.first)
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

    fun convertJsonEpisode(countryCode: CountryCode, jsonObject: JsonObject): Episode {
        val episodeMetadata = jsonObject.getAsJsonObject("episode_metadata")

        val animeName = requireNotNull(episodeMetadata.getAsString("series_title")) { "Anime name is null" }
        if (configuration!!.blacklistedSimulcasts.contains(animeName.lowercase())) throw AnimeException("\"$animeName\" is blacklisted")

        val eligibleRegion =
            requireNotNull(episodeMetadata.getAsString("eligible_region")) { "Eligible region is null" }
        if (!eligibleRegion.contains(countryCode.name)) throw EpisodeNotAvailableInCountryException("Episode of $animeName is not available in ${countryCode.name}")

        val audio = episodeMetadata.getAsString("audio_locale")?.takeIf { it.isNotBlank() }
        val isDubbed = audio == countryCode.locale
        val subtitles = episodeMetadata.getAsJsonArray("subtitle_locales").map { it.asString!! }

        if (!isDubbed && (subtitles.isEmpty() || !subtitles.contains(countryCode.locale))) throw EpisodeNoSubtitlesOrVoiceException(
            "Episode is not available in ${countryCode.name} with subtitles or voice"
        )

        val langType = if (isDubbed) LangType.VOICE else LangType.SUBTITLES

        val id = requireNotNull(jsonObject.getAsString("external_id")?.split(".")?.last()) { "Id is null" }
        val hash = "${countryCode}-${getPlatform()}-$id-$langType"

        if (hashCache.contains(hash)) throw EpisodeAlreadyReleasedException()

        val releaseDate =
            requireNotNull(
                episodeMetadata.getAsString("premium_available_date")
                    ?.let { ZonedDateTime.parse(it) }) { "Release date is null" }

        val season = episodeMetadata.getAsInt("season_number") ?: 1
        val number = episodeMetadata.getAsInt("episode_number") ?: -1
        val seasonSlugTitle = episodeMetadata.getAsString("season_slug_title")

        val episodeType = if (seasonSlugTitle?.contains("movie", true) == true)
            EpisodeType.FILM
        else if (number == -1)
            EpisodeType.SPECIAL
        else
            EpisodeType.EPISODE

        val title = jsonObject.getAsString("title")
        val url = "https://www.crunchyroll.com/media-$id"

        val thumbnailArray = jsonObject.getAsJsonObject("images")?.getAsJsonArray("thumbnail")
        val biggestImage = thumbnailArray?.get(0)?.asJsonArray?.maxByOrNull { it.asJsonObject.getAsInt("width") ?: 0 }
        val image = biggestImage?.asJsonObject?.getAsString("source")?.takeIf { it.isNotBlank() }
            ?: Constant.DEFAULT_IMAGE_PREVIEW

        var duration = episodeMetadata.getAsLong("duration_ms", -1000) / 1000

        val checkCrunchyrollSimulcasts =
            configCacheService.getValueAsBoolean(ConfigPropertyKey.CHECK_CRUNCHYROLL_SIMULCASTS, true)
        val isConfigurationSimulcast = configuration!!.simulcasts.any { it.name.lowercase() == animeName.lowercase() }

        if (checkCrunchyrollSimulcasts && !(isConfigurationSimulcast || simulcasts[countryCode]!!.contains(animeName.lowercase())))
            throw AnimeNotSimulcastedException("\"$animeName\" is not simulcasted")

        val description = jsonObject.getAsString("description")?.replace('\n', ' ')?.takeIf { it.isNotBlank() }
        val animeId = requireNotNull(episodeMetadata.getAsString("series_id")) { "Anime id is null" }
        val crunchyrollAnimeContent = animeInfoCache[CountryCodeAnimeIdKeyCache(countryCode, animeId)]!!

        if (!checkCrunchyrollSimulcasts && !(isConfigurationSimulcast || crunchyrollAnimeContent.simulcast))
            throw AnimeNotSimulcastedException("\"$animeName\" is not simulcasted")

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
                description = crunchyrollAnimeContent.description,
                slug = StringUtils.toSlug(StringUtils.getShortName(animeName)),
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
            duration = duration,
            description = description
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

        val images = jsonObject.getAsJsonArray("media:thumbnail")
        val biggestImage = images?.maxByOrNull { it.asJsonObject.getAsInt("width") ?: 0 }
        val image =
            biggestImage?.asJsonObject?.getAsString("url")?.takeIf { it.isNotBlank() } ?: Constant.DEFAULT_IMAGE_PREVIEW

        var duration = jsonObject.getAsLong("crunchyroll:duration", -1)

        var isSimulcasted =
            simulcasts[countryCode]!!.contains(animeName.lowercase()) || configuration!!.simulcasts.map { it.name.lowercase() }
                .contains(animeName.lowercase())
        isSimulcasted = isSimulcasted || isMovie

        val description = jsonObject.getAsString("description")?.replace('\n', ' ')?.ifBlank { null }

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
                description = crunchyrollAnimeContent.description,
                slug = StringUtils.toSlug(StringUtils.getShortName(animeName)),
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
            duration = duration,
            description = description
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