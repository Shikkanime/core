package fr.shikkanime.platforms

import com.google.gson.JsonObject
import com.google.inject.Inject
import fr.shikkanime.caches.CountryCodeDisneyPlusSimulcastKeyCache
import fr.shikkanime.entities.Config
import fr.shikkanime.entities.enums.ConfigPropertyKey
import fr.shikkanime.entities.enums.CountryCode
import fr.shikkanime.entities.enums.EpisodeType
import fr.shikkanime.entities.enums.Platform
import fr.shikkanime.exceptions.AnimeException
import fr.shikkanime.platforms.configuration.DisneyPlusConfiguration
import fr.shikkanime.services.caches.ConfigCacheService
import fr.shikkanime.utils.MapCache
import fr.shikkanime.utils.ObjectParser.getAsInt
import fr.shikkanime.utils.ObjectParser.getAsLong
import fr.shikkanime.utils.ObjectParser.getAsString
import fr.shikkanime.utils.isEqualOrAfter
import fr.shikkanime.utils.normalize
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
    AbstractPlatform<DisneyPlusConfiguration, CountryCodeDisneyPlusSimulcastKeyCache, Pair<JsonObject, List<JsonObject>>>() {
    @Inject
    private lateinit var configCacheService: ConfigCacheService

    override fun getPlatform(): Platform = Platform.DISN

    override fun getConfigurationClass() = DisneyPlusConfiguration::class.java

    private val identifiers =
        MapCache<CountryCode, String>(
            "DisneyPlusPlatform.identifiers",
            duration = Duration.ofHours(3).plusMinutes(30),
            classes = listOf(Config::class.java)
        ) {
            return@MapCache runBlocking {
                DisneyPlusWrapper.getAccessToken(
                    configCacheService.getValueAsString(ConfigPropertyKey.DISNEY_PLUS_AUTHORIZATION),
                    configCacheService.getValueAsString(ConfigPropertyKey.DISNEY_PLUS_REFRESH_TOKEN)
                )
            }
        }

    override suspend fun fetchApiContent(
        key: CountryCodeDisneyPlusSimulcastKeyCache,
        zonedDateTime: ZonedDateTime
    ): Pair<JsonObject, List<JsonObject>> {
        val accessToken = identifiers[key.countryCode]!!
        val (animeDetails, seasons) = DisneyPlusWrapper.getAnimeDetailsWithSeasons(
            accessToken,
            key.disneyPlusSimulcast.name
        )
        val episodes = seasons.flatMap { DisneyPlusWrapper.getEpisodes(accessToken, it) }
        return animeDetails to episodes
    }

    override fun fetchEpisodes(zonedDateTime: ZonedDateTime, bypassFileContent: File?): List<Episode> {
        val list = mutableListOf<Episode>()

        configuration!!.availableCountries.forEach { countryCode ->
            configuration!!.simulcasts.filter {
                it.releaseDay == zonedDateTime.dayOfWeek.value && zonedDateTime.toLocalTime()
                    .isEqualOrAfter(LocalTime.parse(it.releaseTime))
            }.forEach { simulcast ->
                val (animeDetails, episodes) = getApiContent(
                    CountryCodeDisneyPlusSimulcastKeyCache(
                        countryCode,
                        simulcast
                    ), zonedDateTime
                )

                episodes.forEach {
                    try {
                        list.add(convertEpisode(countryCode, simulcast, animeDetails, it, zonedDateTime))
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
        animeDetails: JsonObject,
        jsonObject: JsonObject,
        zonedDateTime: ZonedDateTime
    ): Episode {
        val animeName = animeDetails.getAsString("title") ?: throw Exception("Anime name is null")
        val tileObject = animeDetails.getAsJsonObject("artwork")?.getAsJsonObject("standard")?.getAsJsonObject("tile")
            ?: throw Exception("Tile object is null")
        val animeImageId =
            tileObject.getAsJsonObject("0.71")?.getAsString("imageId") ?: throw Exception("Anime image is null")
        val animeImage = DisneyPlusWrapper.getImageUrl(animeImageId)
        val animeBannerId =
            tileObject.getAsJsonObject("1.33")?.getAsString("imageId") ?: throw Exception("Anime image is null")
        val animeBanner = DisneyPlusWrapper.getImageUrl(animeBannerId)
        val animeDescription =
            animeDetails.getAsJsonObject("description")?.getAsString("full")

        val visualsObject = jsonObject.getAsJsonObject("visuals")

        val season = requireNotNull(visualsObject.getAsInt("seasonNumber")) { "Season is null" }
        val number = visualsObject.getAsInt("episodeNumber") ?: -1
        val oldId = jsonObject.getAsJsonArray("actions")[0].asJsonObject
            .getAsJsonObject("legacyPartnerFeed").getAsString("dmcContentId") ?: throw Exception("Old id is null")
        val id = jsonObject.getAsString("id") ?: throw Exception("Id is null")
        val title = visualsObject.getAsString("episodeTitle")
        val url = "https://www.disneyplus.com/${countryCode.locale.lowercase()}/play/$id"
        val imageId =
            visualsObject.getAsJsonObject("artwork")?.getAsJsonObject("standard")?.getAsJsonObject("thumbnail")
            ?.getAsJsonObject("1.78")?.getAsString("imageId") ?: throw Exception("Image is null")
        val image = DisneyPlusWrapper.getImageUrl(imageId)
        var duration = visualsObject.getAsLong("durationMs", -1)

        if (duration != -1L) {
            duration /= 1000
        }

        val description =
            visualsObject.getAsJsonObject("description")?.getAsString("medium")
        val releaseDateTimeUTC =
            zonedDateTime.withUTC().format(DateTimeFormatter.ISO_LOCAL_DATE) + "T${simulcast.releaseTime}Z"
        val releaseDateTime = ZonedDateTime.parse(releaseDateTimeUTC)

        if (hashCache.contains(oldId) || hashCache.contains(id)) {
            throw AnimeException("Episode already exists")
        }

        hashCache.addAll(mutableListOf(oldId, id))

        return Episode(
            countryCode = countryCode,
            animeId = simulcast.name,
            anime = animeName,
            animeImage = animeImage,
            animeBanner = animeBanner,
            animeDescription = animeDescription.normalize(),
            releaseDateTime = releaseDateTime,
            episodeType = EpisodeType.EPISODE,
            season = season,
            number = number,
            duration = duration,
            title = title.normalize(),
            description = description.normalize(),
            image = image,
            platform = getPlatform(),
            audioLocale = "ja-JP",
            id = id,
            url = url,
            uncensored = false,
            original = true,
        )
    }
}