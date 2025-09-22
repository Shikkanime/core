package fr.shikkanime.platforms

import fr.shikkanime.caches.CountryCodeNetflixSimulcastKeyCache
import fr.shikkanime.entities.enums.CountryCode
import fr.shikkanime.entities.enums.ImageType
import fr.shikkanime.entities.enums.Platform
import fr.shikkanime.platforms.configuration.NetflixConfiguration
import fr.shikkanime.wrappers.factories.AbstractNetflixWrapper
import fr.shikkanime.wrappers.impl.NetflixWrapper
import java.io.File
import java.time.ZonedDateTime

class NetflixPlatform : AbstractPlatform<NetflixConfiguration, CountryCodeNetflixSimulcastKeyCache, List<AbstractPlatform.Episode>>() {
    override fun getPlatform(): Platform = Platform.NETF

    override fun getConfigurationClass() = NetflixConfiguration::class.java

    override suspend fun fetchApiContent(
        key: CountryCodeNetflixSimulcastKeyCache,
        zonedDateTime: ZonedDateTime
    ) = NetflixWrapper.getEpisodesByShowId(key.countryCode.locale, key.netflixSimulcast.name.toInt())
        .flatMap { video ->
            val audioLocales = video.audioLocales.ifEmpty { key.netflixSimulcast.audioLocales }
            audioLocales.map { audioLocale ->
                convertEpisode(
                    key.countryCode,
                    key.netflixSimulcast.image,
                    video,
                    audioLocale
                ).apply {
                    if (key.netflixSimulcast.audioLocaleHasDelay.contains(audioLocale)) {
                        releaseDateTime = zonedDateTime
                    }
                }
            }
        }

    override fun fetchEpisodes(zonedDateTime: ZonedDateTime, bypassFileContent: File?) = configuration!!.availableCountries.flatMap { countryCode ->
        configuration!!.simulcasts
            .filter { it.releaseDay == 0 || it.releaseDay == zonedDateTime.dayOfWeek.value }
            .flatMap { simulcast ->
                getApiContent(CountryCodeNetflixSimulcastKeyCache(countryCode, simulcast), zonedDateTime)
            }
    }


    fun convertEpisode(
        countryCode: CountryCode,
        showImage: String,
        episode: AbstractNetflixWrapper.Episode,
        audioLocale: String,
    ) = Episode(
        countryCode = countryCode,
        animeId = episode.show.id.toString(),
        anime = episode.show.name,
        animeAttachments = mapOf(
            ImageType.THUMBNAIL to (episode.show.thumbnail ?: showImage),
            ImageType.BANNER to episode.show.banner,
            ImageType.CAROUSEL to episode.show.carousel
        ),
        animeDescription = episode.show.description,
        releaseDateTime = requireNotNull(episode.releaseDateTime) { "Release date is null" },
        episodeType = episode.episodeType,
        seasonId = episode.season.toString(),
        season = episode.season,
        number = episode.number,
        duration = episode.duration,
        title = episode.title,
        description = episode.description,
        image = episode.image,
        platform = getPlatform(),
        audioLocale = audioLocale,
        id = episode.id.toString(),
        url = episode.url,
        uncensored = false,
        original = true,
    )
}