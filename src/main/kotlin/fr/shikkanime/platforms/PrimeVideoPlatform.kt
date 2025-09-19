package fr.shikkanime.platforms

import fr.shikkanime.caches.CountryCodePrimeVideoSimulcastKeyCache
import fr.shikkanime.entities.enums.CountryCode
import fr.shikkanime.entities.enums.EpisodeType
import fr.shikkanime.entities.enums.ImageType
import fr.shikkanime.entities.enums.Platform
import fr.shikkanime.exceptions.EpisodeNoSubtitlesOrVoiceException
import fr.shikkanime.platforms.configuration.PrimeVideoConfiguration
import fr.shikkanime.wrappers.factories.AbstractPrimeVideoWrapper
import fr.shikkanime.wrappers.impl.PrimeVideoWrapper
import java.io.File
import java.time.LocalTime
import java.time.ZonedDateTime
import java.util.logging.Level

class PrimeVideoPlatform :
    AbstractPlatform<PrimeVideoConfiguration, CountryCodePrimeVideoSimulcastKeyCache, List<AbstractPlatform.Episode>>() {
    override fun getPlatform(): Platform = Platform.PRIM

    override fun getConfigurationClass() = PrimeVideoConfiguration::class.java

    override suspend fun fetchApiContent(
        key: CountryCodePrimeVideoSimulcastKeyCache,
        zonedDateTime: ZonedDateTime
    ): List<Episode> {
        val episodes = PrimeVideoWrapper.getEpisodesByShowId(key.countryCode, key.primeVideoSimulcast.name)

        return episodes.flatMap {
            try {
                convertEpisode(
                    key.countryCode,
                    key.primeVideoSimulcast.image,
                    it,
                    zonedDateTime.minusMinutes(1),
                    key.primeVideoSimulcast.episodeType
                )
            } catch (e: Exception) {
                logger.log(Level.SEVERE, "Error on converting episode", e)
                emptyList()
            }
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
    ): List<Episode> {
        val isDubbed = countryCode.locale in episode.audioLocales
        val subtitles = episode.subtitleLocales

        if (!isDubbed && (subtitles.isEmpty() || countryCode.locale !in subtitles))
            throw EpisodeNoSubtitlesOrVoiceException("Episode is not available in ${countryCode.name} with subtitles or voice")

        return episode.audioLocales.map {
            Episode(
                countryCode = countryCode,
                animeId = episode.show.id,
                anime = episode.show.name,
                animeAttachments = mapOf(
                    ImageType.THUMBNAIL to showImage,
                    ImageType.BANNER to episode.show.banner,
                    ImageType.CAROUSEL to episode.show.carousel
                ),
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
}