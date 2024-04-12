package fr.shikkanime.platforms

import com.google.gson.JsonObject
import com.google.inject.Inject
import fr.shikkanime.caches.CountryCodeDisneyPlusSimulcastKeyCache
import fr.shikkanime.entities.Anime
import fr.shikkanime.entities.Config
import fr.shikkanime.entities.Episode
import fr.shikkanime.entities.enums.*
import fr.shikkanime.exceptions.AnimeException
import fr.shikkanime.platforms.configuration.DisneyPlusConfiguration
import fr.shikkanime.services.caches.ConfigCacheService
import fr.shikkanime.utils.MapCache
import fr.shikkanime.utils.ObjectParser.getAsInt
import fr.shikkanime.utils.ObjectParser.getAsLong
import fr.shikkanime.utils.ObjectParser.getAsString
import fr.shikkanime.utils.StringUtils
import fr.shikkanime.utils.isEqualOrAfter
import fr.shikkanime.utils.withUTC
import fr.shikkanime.wrappers.DisneyPlusWrapper
import kotlinx.coroutines.runBlocking
import java.io.File
import java.time.Duration
import java.time.LocalTime
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.logging.Level

class DisneyPlusPlatform :
    AbstractPlatform<DisneyPlusConfiguration, CountryCodeDisneyPlusSimulcastKeyCache, List<JsonObject>>() {
    @Inject
    private lateinit var configCacheService: ConfigCacheService

    override fun getPlatform(): Platform = Platform.DISN

    override fun getConfigurationClass() = DisneyPlusConfiguration::class.java

    private val identifiers = MapCache<CountryCode, String>(Duration.ofHours(3).plusMinutes(30), listOf(Config::class.java)) {
        return@MapCache runBlocking {
            DisneyPlusWrapper.getAccessToken(
                configCacheService.getValueAsString(ConfigPropertyKey.DISNEY_PLUS_AUTHORIZATION),
                configCacheService.getValueAsString(ConfigPropertyKey.DISNEY_PLUS_REFRESH_TOKEN)
            )
        }
    }

    private val seasons = MapCache<CountryCodeDisneyPlusSimulcastKeyCache, List<String>>(Duration.ofDays(1)) {
        val accessToken = identifiers[it.countryCode]!!
        return@MapCache runBlocking { DisneyPlusWrapper.getSeasons(accessToken, it.countryCode, it.disneyPlusSimulcast.name) }
    }

    override suspend fun fetchApiContent(
        key: CountryCodeDisneyPlusSimulcastKeyCache,
        zonedDateTime: ZonedDateTime
    ) = this.seasons[key]!!.flatMap { season ->
        DisneyPlusWrapper.getEpisodes(identifiers[key.countryCode]!!, key.countryCode, season)
    }

    override fun fetchEpisodes(zonedDateTime: ZonedDateTime, bypassFileContent: File?): List<Episode> {
        val list = mutableListOf<Episode>()

        configuration!!.availableCountries.forEach { countryCode ->
            configuration!!.simulcasts.filter {
                it.releaseDay == zonedDateTime.dayOfWeek.value && zonedDateTime.toLocalTime()
                    .isEqualOrAfter(LocalTime.parse(it.releaseTime))
            }.forEach { simulcast ->
                val api = getApiContent(CountryCodeDisneyPlusSimulcastKeyCache(countryCode, simulcast), zonedDateTime)

                api.forEach {
                    try {
                        list.add(convertEpisode(countryCode, simulcast, it, zonedDateTime))
                    } catch (_: AnimeException) {
                        // Ignore
                    } catch (e: Exception) {
                        logger.log(Level.SEVERE, "Error on converting episode", e)
                    }
                }
            }
        }

        return list
    }

    private fun convertEpisode(
        countryCode: CountryCode,
        simulcast: DisneyPlusConfiguration.DisneyPlusSimulcast,
        jsonObject: JsonObject,
        zonedDateTime: ZonedDateTime
    ): Episode {
        val texts = jsonObject.getAsJsonObject("text")
        val titles = texts?.getAsJsonObject("title")?.getAsJsonObject("full")
        val descriptions = texts.getAsJsonObject("description")?.getAsJsonObject("medium")
        val animeName = titles?.getAsJsonObject("series")?.getAsJsonObject("default")?.getAsString("content")
            ?: throw Exception("Anime name is null")

        val animeImage = jsonObject.getAsJsonObject("image")?.getAsJsonObject("tile")?.getAsJsonObject("0.71")
            ?.getAsJsonObject("series")?.getAsJsonObject("default")?.getAsString("url")
            ?: throw Exception("Anime image is null")
        val animeBanner = jsonObject.getAsJsonObject("image")?.getAsJsonObject("tile")?.getAsJsonObject("1.33")
            ?.getAsJsonObject("series")?.getAsJsonObject("default")?.getAsString("url")
            ?: throw Exception("Anime image is null")
        val animeDescription = descriptions?.getAsJsonObject("series")
            ?.getAsJsonObject("default")?.getAsString("content")?.replace('\n', ' ') ?: ""

        val season = jsonObject.getAsInt("seasonSequenceNumber")

        val number = jsonObject.getAsInt("episodeSequenceNumber")

        val id = jsonObject.getAsString("contentId")

        val title =
            titles.getAsJsonObject("program")?.getAsJsonObject("default")?.getAsString("content")?.ifBlank { null }

        val url = "https://www.disneyplus.com/${countryCode.locale.lowercase()}/video/$id"

        val image = jsonObject.getAsJsonObject("image")?.getAsJsonObject("thumbnail")?.getAsJsonObject("1.78")
            ?.getAsJsonObject("program")?.getAsJsonObject("default")?.getAsString("url")
            ?: throw Exception("Image is null")

        var duration = jsonObject.getAsJsonObject("mediaMetadata")?.getAsLong("runtimeMillis", -1) ?: -1

        if (duration != -1L) {
            duration /= 1000
        }

        val description = descriptions?.getAsJsonObject("program")
            ?.getAsJsonObject("default")?.getAsString("content")?.replace('\n', ' ')?.ifBlank { null }

        val langType = LangType.SUBTITLES
        val releaseDateTimeUTC = zonedDateTime.withUTC()
            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) + "T${simulcast.releaseTime}Z"
        val releaseDateTime = ZonedDateTime.parse(releaseDateTimeUTC)

        return Episode(
            platform = getPlatform(),
            anime = Anime(
                countryCode = countryCode,
                name = animeName,
                releaseDateTime = releaseDateTime,
                image = animeImage,
                banner = animeBanner,
                description = animeDescription,
                slug = StringUtils.toSlug(StringUtils.getShortName(animeName)),
            ),
            episodeType = EpisodeType.EPISODE,
            langType = langType,
            audioLocale = "ja-JP",
            hash = StringUtils.getHash(countryCode, getPlatform(), id.toString(), langType),
            releaseDateTime = releaseDateTime,
            season = season,
            number = number,
            title = title,
            url = url,
            image = image,
            duration = duration,
            description = description,
        )
    }
}