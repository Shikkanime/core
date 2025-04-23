package fr.shikkanime.platforms

import fr.shikkanime.caches.CountryCodePrimeVideoSimulcastKeyCache
import fr.shikkanime.entities.enums.CountryCode
import fr.shikkanime.entities.enums.EpisodeType
import fr.shikkanime.entities.enums.Platform
import fr.shikkanime.platforms.configuration.PrimeVideoConfiguration
import fr.shikkanime.wrappers.factories.AbstractPrimeVideoWrapper
import fr.shikkanime.wrappers.impl.PrimeVideoWrapper
import java.io.File
import java.time.LocalTime
import java.time.ZonedDateTime

class PrimeVideoPlatform :
    AbstractPlatform<PrimeVideoConfiguration, CountryCodePrimeVideoSimulcastKeyCache, List<AbstractPlatform.Episode>>() {
    override fun getPlatform(): Platform = Platform.PRIM

    override fun getConfigurationClass() = PrimeVideoConfiguration::class.java

    override suspend fun fetchApiContent(
        key: CountryCodePrimeVideoSimulcastKeyCache,
        zonedDateTime: ZonedDateTime
    ): List<Episode> {
        val episodes = PrimeVideoWrapper.getEpisodesByShowId(key.countryCode.locale, key.primeVideoSimulcast.name)

        return episodes.flatMap {
            convertEpisode(
                key.countryCode,
                key.primeVideoSimulcast.image,
                it,
                zonedDateTime.minusMinutes(1),
                key.primeVideoSimulcast.episodeType
            )
        }
    }

    override fun fetchEpisodes(zonedDateTime: ZonedDateTime, bypassFileContent: File?): List<Episode> {
        val list = mutableListOf<Episode>()

        configuration!!.availableCountries.forEach { countryCode ->
            configuration!!.simulcasts.filter {
                (it.releaseDay == 0 || it.releaseDay == zonedDateTime.dayOfWeek.value) &&
                        (it.releaseTime.isBlank() || zonedDateTime.toLocalTime() >= LocalTime.parse(it.releaseTime))
            }
                .forEach { simulcast ->
                    list.addAll(getApiContent(CountryCodePrimeVideoSimulcastKeyCache(countryCode, simulcast), zonedDateTime))
                }
        }

        return list
    }

    fun convertEpisode(
        countryCode: CountryCode,
        showImage: String,
        episode: AbstractPrimeVideoWrapper.Episode,
        zonedDateTime: ZonedDateTime,
        episodeType: EpisodeType,
    ) = episode.audioLocales.map {
        Episode(
            countryCode = countryCode,
            animeId = episode.show.id,
            anime = episode.show.name,
            animeImage = showImage,
            animeBanner = episode.show.banner,
            animeDescription = episode.show.description,
            releaseDateTime = zonedDateTime,
            episodeType = episodeType,
            seasonId = episode.season.toString(),
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