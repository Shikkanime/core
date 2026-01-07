package fr.shikkanime.platforms

import fr.shikkanime.caches.CountryCodeReleaseDayPlatformSimulcastKeyCache
import fr.shikkanime.entities.enums.CountryCode
import fr.shikkanime.entities.enums.ImageType
import fr.shikkanime.entities.enums.Platform
import fr.shikkanime.platforms.configuration.NetflixConfiguration
import fr.shikkanime.wrappers.factories.AbstractNetflixWrapper
import fr.shikkanime.wrappers.impl.NetflixWrapper
import java.io.File
import java.time.ZonedDateTime

class NetflixPlatform : AbstractPlatform<NetflixConfiguration, CountryCodeReleaseDayPlatformSimulcastKeyCache, List<AbstractPlatform.Episode>>() {
    override fun getPlatform(): Platform = Platform.NETF

    override fun getConfigurationClass() = NetflixConfiguration::class.java

    override suspend fun fetchApiContent(
        key: CountryCodeReleaseDayPlatformSimulcastKeyCache,
        zonedDateTime: ZonedDateTime
    ) = NetflixWrapper.getEpisodesByShowId(key.countryCode, key.releaseDayPlatformSimulcast.name.toInt())
        .flatMap { video -> convertEpisode(key.countryCode, video) }

    override suspend fun fetchEpisodes(zonedDateTime: ZonedDateTime, bypassFileContent: File?) = configuration!!.availableCountries.flatMap { countryCode ->
        configuration!!.simulcasts.filter { it.canBeFetch(zonedDateTime) }
            .flatMap { simulcast -> getApiContent(CountryCodeReleaseDayPlatformSimulcastKeyCache(countryCode, simulcast), zonedDateTime) }
    }


    fun convertEpisode(
        countryCode: CountryCode,
        episode: AbstractNetflixWrapper.Episode,
    ): List<Episode> = episode.audioLocales.map {
        Episode(
            countryCode = countryCode,
            animeId = episode.show.id.toString(),
            anime = episode.show.name,
            animeAttachments = buildMap {
                episode.show.thumbnail?.let { image -> put(ImageType.THUMBNAIL, image) }
                put(ImageType.BANNER, episode.show.banner)
                put(ImageType.CAROUSEL, episode.show.carousel)
                put(ImageType.TITLE, episode.show.title)
            },
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
            audioLocale = it,
            id = episode.id.toString(),
            url = episode.url,
            uncensored = false,
            original = true,
        )
    }
}