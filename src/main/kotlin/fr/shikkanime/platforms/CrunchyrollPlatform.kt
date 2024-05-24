package fr.shikkanime.platforms

import com.google.gson.JsonObject
import com.google.inject.Inject
import fr.shikkanime.caches.CountryCodeIdKeyCache
import fr.shikkanime.entities.enums.ConfigPropertyKey
import fr.shikkanime.entities.enums.CountryCode
import fr.shikkanime.entities.enums.EpisodeType
import fr.shikkanime.entities.enums.Platform
import fr.shikkanime.exceptions.*
import fr.shikkanime.platforms.configuration.CrunchyrollConfiguration
import fr.shikkanime.services.caches.ConfigCacheService
import fr.shikkanime.utils.Constant
import fr.shikkanime.utils.MapCache
import fr.shikkanime.utils.ObjectParser.getAsBoolean
import fr.shikkanime.utils.ObjectParser.getAsInt
import fr.shikkanime.utils.ObjectParser.getAsLong
import fr.shikkanime.utils.ObjectParser.getAsString
import fr.shikkanime.wrappers.CrunchyrollWrapper
import kotlinx.coroutines.runBlocking
import java.io.File
import java.time.Duration
import java.time.ZonedDateTime
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

    private val identifiers = MapCache<CountryCode, String>(Duration.ofMinutes(30)) {
        val token = runBlocking { CrunchyrollWrapper.getAnonymousAccessToken() }
        return@MapCache token
    }

    val simulcasts = MapCache<CountryCode, Set<String>>(Duration.ofHours(1)) {
        val simulcastSeries = mutableSetOf<String>()
        val accessToken = identifiers[it]!!
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
        logger.info(simulcastSeries.joinToString(", "))
        return@MapCache simulcastSeries
    }

    private val animeInfoCache = MapCache<CountryCodeIdKeyCache, CrunchyrollAnimeContent>(Duration.ofDays(1)) {
        val token = identifiers[it.countryCode]!!
        val `object` = runBlocking {
            CrunchyrollWrapper.getSeries(
                it.countryCode.locale,
                token,
                it.id
            )
        }[0]

        val postersTall = `object`.getAsJsonObject("images").getAsJsonArray("poster_tall")[0].asJsonArray
        val postersWide = `object`.getAsJsonObject("images").getAsJsonArray("poster_wide")[0].asJsonArray
        val image =
            postersTall?.maxByOrNull { poster -> poster.asJsonObject.getAsInt("width")!! }?.asJsonObject?.getAsString(
                "source"
            )
        val banner =
            postersWide?.maxByOrNull { poster -> poster.asJsonObject.getAsInt("width")!! }?.asJsonObject?.getAsString(
                "source"
            )
        val description = `object`.getAsString("description")
        val simulcast = `object`.getAsBoolean("is_simulcast") ?: false

        if (image.isNullOrEmpty()) {
            throw Exception("Image is null or empty")
        }

        if (banner.isNullOrEmpty()) {
            throw Exception("Banner is null or empty")
        }

        return@MapCache CrunchyrollAnimeContent(image, banner, description, simulcast)
    }

    override fun getPlatform(): Platform = Platform.CRUN

    override fun getConfigurationClass() = CrunchyrollConfiguration::class.java

    override suspend fun fetchApiContent(key: CountryCode, zonedDateTime: ZonedDateTime): List<JsonObject> {
        return CrunchyrollWrapper.getBrowse(
            key.locale,
            identifiers[key]!!,
            size = configCacheService.getValueAsInt(ConfigPropertyKey.CRUNCHYROLL_FETCH_API_SIZE, 25)
        )
    }

    override fun fetchEpisodes(zonedDateTime: ZonedDateTime, bypassFileContent: File?): List<Episode> {
        val list = mutableListOf<Episode>()

        configuration!!.availableCountries.forEach { countryCode ->
            val api = parseAPIContent(bypassFileContent, countryCode, "data", zonedDateTime)

            api.forEach {
                try {
                    list.add(convertEpisode(countryCode, it))
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

    private fun convertEpisode(
        countryCode: CountryCode,
        jsonObject: JsonObject,
        needSimulcast: Boolean = true
    ): Episode {
        val episodeMetadata = jsonObject.getAsJsonObject("episode_metadata")

        val animeName = requireNotNull(episodeMetadata.getAsString("series_title")) { "Anime name is null" }
        if (configuration!!.blacklistedSimulcasts.contains(animeName.lowercase())) throw AnimeException("\"$animeName\" is blacklisted")

        val eligibleRegion =
            requireNotNull(episodeMetadata.getAsString("eligible_region")) { "Eligible region is null" }
        if (!eligibleRegion.contains(countryCode.name)) throw EpisodeNotAvailableInCountryException("Episode of $animeName is not available in ${countryCode.name}")

        val audioLocale = requireNotNull(episodeMetadata.getAsString("audio_locale")) { "Audio locale is null" }
        val isDubbed = audioLocale == countryCode.locale
        val subtitles = episodeMetadata.getAsJsonArray("subtitle_locales").map { it.asString!! }

        if (!isDubbed && (subtitles.isEmpty() || !subtitles.contains(countryCode.locale))) throw EpisodeNoSubtitlesOrVoiceException(
            "Episode is not available in ${countryCode.name} with subtitles or voice"
        )

        val id = requireNotNull(jsonObject.getAsString("id")) { "Id is null" }

        val releaseDate =
            requireNotNull(
                episodeMetadata.getAsString("premium_available_date")
                    ?.let { ZonedDateTime.parse(it) }) { "Release date is null" }

        val season = episodeMetadata.getAsInt("season_number") ?: 1
        val (number, episodeType) = getNumberAndEpisodeType(episodeMetadata)

        val title = jsonObject.getAsString("title")
        val slugTitle = jsonObject.getAsString("slug_title")
        val url = CrunchyrollWrapper.buildUrl(countryCode, id, slugTitle)

        val thumbnailArray = jsonObject.getAsJsonObject("images")?.getAsJsonArray("thumbnail")
        val biggestImage = thumbnailArray?.get(0)?.asJsonArray?.maxByOrNull { it.asJsonObject.getAsInt("width") ?: 0 }
        val image = biggestImage?.asJsonObject?.getAsString("source")?.takeIf { it.isNotBlank() }
            ?: Constant.DEFAULT_IMAGE_PREVIEW

        val duration = episodeMetadata.getAsLong("duration_ms", -1000) / 1000

        val checkCrunchyrollSimulcasts =
            configCacheService.getValueAsBoolean(ConfigPropertyKey.CHECK_CRUNCHYROLL_SIMULCASTS, true)
        val isConfigurationSimulcast = configuration!!.simulcasts.any { it.name.lowercase() == animeName.lowercase() }

        if (needSimulcast && checkCrunchyrollSimulcasts && !(isConfigurationSimulcast || simulcasts[countryCode]!!.contains(
                animeName.lowercase()
            ))
        )
            throw AnimeNotSimulcastedException("\"$animeName\" is not simulcasted")

        val description = jsonObject.getAsString("description")?.replace('\n', ' ')?.takeIf { it.isNotBlank() }
        val animeId = requireNotNull(episodeMetadata.getAsString("series_id")) { "Anime id is null" }
        val crunchyrollAnimeContent = animeInfoCache[CountryCodeIdKeyCache(countryCode, animeId)]!!

        if (needSimulcast && !checkCrunchyrollSimulcasts && !(isConfigurationSimulcast || crunchyrollAnimeContent.simulcast))
            throw AnimeNotSimulcastedException("\"$animeName\" is not simulcasted")

        return Episode(
            countryCode = countryCode,
            anime = animeName,
            animeImage = crunchyrollAnimeContent.image,
            animeBanner = crunchyrollAnimeContent.banner,
            animeDescription = crunchyrollAnimeContent.description,
            releaseDateTime = releaseDate,
            episodeType = episodeType,
            season = season,
            number = number,
            duration = duration,
            title = title,
            description = description,
            image = image,
            platform = getPlatform(),
            audioLocale = audioLocale,
            id = id,
            url = url,
            uncensored = false,
        )
    }

    private fun getNumberAndEpisodeType(episodeMetadata: JsonObject): Pair<Int, EpisodeType> {
        val seasonSlugTitle = episodeMetadata.getAsString("season_slug_title")
        val episodeString = episodeMetadata.getAsString("episode")!!
        var number = episodeMetadata.getAsInt("episode_number") ?: -1
        val specialEpisodeRegex = "SP(\\d*)".toRegex()

        var episodeType = when {
            seasonSlugTitle?.contains("movie", true) == true -> EpisodeType.FILM
            number == -1 -> EpisodeType.SPECIAL
            else -> EpisodeType.EPISODE
        }

        specialEpisodeRegex.find(episodeString)?.let {
            episodeType = EpisodeType.SPECIAL
            number = it.groupValues[1].toIntOrNull() ?: -1
        }

        return Pair(number, episodeType)
    }
}