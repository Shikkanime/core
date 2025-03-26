package fr.shikkanime.platforms

import com.google.inject.Inject
import fr.shikkanime.caches.CountryCodeReleaseDayPlatformSimulcastKeyCache
import fr.shikkanime.entities.enums.ConfigPropertyKey
import fr.shikkanime.entities.enums.CountryCode
import fr.shikkanime.entities.enums.EpisodeType
import fr.shikkanime.entities.enums.Platform
import fr.shikkanime.exceptions.AnimeException
import fr.shikkanime.platforms.configuration.DisneyPlusConfiguration
import fr.shikkanime.services.caches.ConfigCacheService
import fr.shikkanime.wrappers.factories.AbstractDisneyPlusWrapper
import fr.shikkanime.wrappers.factories.AbstractDisneyPlusWrapper.Episode
import fr.shikkanime.wrappers.impl.DisneyPlusWrapper
import java.io.File
import java.time.ZonedDateTime
import java.util.logging.Level

class DisneyPlusPlatform : AbstractPlatform<DisneyPlusConfiguration, CountryCodeReleaseDayPlatformSimulcastKeyCache, List<Episode>>() {
    @Inject
    private lateinit var configCacheService: ConfigCacheService

    override fun getPlatform(): Platform = Platform.DISN

    override fun getConfigurationClass() = DisneyPlusConfiguration::class.java

    override suspend fun fetchApiContent(
        key: CountryCodeReleaseDayPlatformSimulcastKeyCache,
        zonedDateTime: ZonedDateTime
    ) = DisneyPlusWrapper.getEpisodesByShowId(
        key.countryCode.locale,
        key.releaseDayPlatformSimulcast.name,
        configCacheService.getValueAsBoolean(ConfigPropertyKey.CHECK_DISNEY_PLUS_AUDIO_LOCALES)
    )

    override fun fetchEpisodes(zonedDateTime: ZonedDateTime, bypassFileContent: File?): List<Episode> {
        val list = mutableListOf<Episode>()

        configuration!!.availableCountries.forEach { countryCode ->
            val locales = listOf("ja-JP", countryCode.locale)

            configuration!!.simulcasts.filter { it.releaseDay == 0 || it.releaseDay == zonedDateTime.dayOfWeek.value }
                .forEach { simulcast ->
                    val episodes = getApiContent(
                        CountryCodeReleaseDayPlatformSimulcastKeyCache(
                            countryCode,
                            simulcast
                        ), zonedDateTime
                    )

                    episodes.forEach {
                        try {
                            list.addAll(
                                it.audioLocales
                                    .filter { it in locales }
                                    .map { locale ->
                                        convertEpisode(
                                            countryCode,
                                            it,
                                            zonedDateTime,
                                            locale,
                                            locale == "ja-JP"
                                        )
                                    }
                            )
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

    fun convertEpisode(
        countryCode: CountryCode,
        episode: AbstractDisneyPlusWrapper.Episode,
        zonedDateTime: ZonedDateTime,
        audioLocale: String = "ja-JP",
        original: Boolean = true
    ) = Episode(
        countryCode = countryCode,
        animeId = episode.show.id,
        anime = episode.show.name,
        animeImage = episode.show.image,
        animeBanner = episode.show.banner,
        animeDescription = episode.show.description,
        releaseDateTime = zonedDateTime,
        episodeType = EpisodeType.EPISODE,
        seasonId = episode.seasonId,
        season = episode.season,
        number = episode.number,
        duration = episode.duration,
        title = episode.title,
        description = episode.description,
        image = episode.image,
        platform = getPlatform(),
        audioLocale = audioLocale,
        id = episode.id,
        url = episode.url,
        uncensored = false,
        original = original,
    )

    fun getDisneyPlusId(identifier: String) =
        "[A-Z]{2}-DISN-(.*)-[A-Z]{2}-[A-Z]{2}".toRegex().find(identifier)?.groupValues?.get(1)
}