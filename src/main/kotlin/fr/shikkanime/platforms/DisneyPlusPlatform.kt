package fr.shikkanime.platforms

import com.google.inject.Inject
import fr.shikkanime.caches.CountryCodeReleaseDayPlatformSimulcastKeyCache
import fr.shikkanime.entities.enums.ConfigPropertyKey
import fr.shikkanime.entities.enums.CountryCode
import fr.shikkanime.entities.enums.EpisodeType
import fr.shikkanime.entities.enums.Platform
import fr.shikkanime.platforms.configuration.DisneyPlusConfiguration
import fr.shikkanime.services.caches.ConfigCacheService
import fr.shikkanime.wrappers.factories.AbstractDisneyPlusWrapper
import fr.shikkanime.wrappers.impl.DisneyPlusWrapper
import java.io.File
import java.time.ZonedDateTime

class DisneyPlusPlatform : AbstractPlatform<DisneyPlusConfiguration, CountryCodeReleaseDayPlatformSimulcastKeyCache, List<AbstractPlatform.Episode>>() {
    @Inject private lateinit var configCacheService: ConfigCacheService

    override fun getPlatform(): Platform = Platform.DISN

    override fun getConfigurationClass() = DisneyPlusConfiguration::class.java

    override suspend fun fetchApiContent(
        key: CountryCodeReleaseDayPlatformSimulcastKeyCache,
        zonedDateTime: ZonedDateTime
    ): List<Episode> {
        val episodes = DisneyPlusWrapper.getEpisodesByShowId(
            key.countryCode.locale,
            key.releaseDayPlatformSimulcast.name,
            configCacheService.getValueAsBoolean(ConfigPropertyKey.CHECK_DISNEY_PLUS_AUDIO_LOCALES)
        )

        return episodes.flatMap { episode ->
            convertEpisode(
                key.countryCode,
                episode,
                zonedDateTime,
            )
        }
    }

    override fun fetchEpisodes(zonedDateTime: ZonedDateTime, bypassFileContent: File?): List<Episode> {
        val list = mutableListOf<Episode>()

        configuration!!.availableCountries.forEach { countryCode ->
            configuration!!.simulcasts.filter { it.releaseDay == 0 || it.releaseDay == zonedDateTime.dayOfWeek.value }
                .forEach { simulcast ->
                    list.addAll(
                        getApiContent(
                            CountryCodeReleaseDayPlatformSimulcastKeyCache(
                                countryCode,
                                simulcast
                            ), zonedDateTime
                        )
                    )
                }
        }

        return list
    }

    fun convertEpisode(
        countryCode: CountryCode,
        episode: AbstractDisneyPlusWrapper.Episode,
        zonedDateTime: ZonedDateTime
    ) = episode.audioLocales.map {
        Episode(
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
            audioLocale = it,
            id = episode.id,
            url = episode.url,
            uncensored = false,
            original = true,
        )
    }
}