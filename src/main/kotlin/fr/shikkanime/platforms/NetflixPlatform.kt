package fr.shikkanime.platforms

import fr.shikkanime.caches.PlatformSimulcastFetchCacheKey
import fr.shikkanime.dtos.AnimePlatformDto
import fr.shikkanime.entities.enums.ConfigPropertyKey
import fr.shikkanime.entities.enums.CountryCode
import fr.shikkanime.entities.enums.ImageType
import fr.shikkanime.entities.enums.Platform
import fr.shikkanime.platforms.configuration.NetflixConfiguration
import fr.shikkanime.platforms.configuration.ReleaseDayPlatformSimulcast
import fr.shikkanime.wrappers.factories.AbstractNetflixWrapper
import fr.shikkanime.wrappers.impl.NetflixWrapper
import fr.shikkanime.wrappers.impl.caches.NetflixCachedWrapper
import java.io.File
import java.time.ZonedDateTime
import java.util.*

class NetflixPlatform : AbstractPlatform<NetflixConfiguration, PlatformSimulcastFetchCacheKey, List<AbstractPlatform.Episode>>() {
    override fun getPlatform(): Platform = Platform.NETF

    override fun getConfigurationClass() = NetflixConfiguration::class.java

    override suspend fun fetchApiContent(
        key: PlatformSimulcastFetchCacheKey,
        zonedDateTime: ZonedDateTime
    ) = NetflixWrapper.getEpisodesByShowId(key.countryCode, key.simulcast.name.toInt())
        .flatMap { video -> convertEpisode(key.countryCode, video) }

    override suspend fun fetchEpisodes(zonedDateTime: ZonedDateTime, bypassFileContent: File?): List<Episode> {
        val list = mutableListOf<Episode>()

        if (configCacheService.getValueAsBoolean(ConfigPropertyKey.NETFLIX_FETCH_LATEST_SHOWS)) {
            getLatestShows(zonedDateTime) {
                val configurationShowIds = configuration!!.simulcasts.map(ReleaseDayPlatformSimulcast::name)
                val databaseShowIds =
                    animePlatformCacheService.findAllByPlatform(getPlatform()).map(AnimePlatformDto::platformId)
                val showIds = (configurationShowIds + databaseShowIds).distinct()

                NetflixWrapper.getLatestShows()
                    .filter { show -> !showIds.contains(show.id.toString()) }
                    .flatMap { show ->
                        configuration!!.availableCountries.map { countryCode ->
                            NetflixCachedWrapper.getShow(
                                countryCode.locale,
                                show.id
                            )
                        }
                    }
                    .filter {
                        it.availabilityStartTime != null && it.genres.contains("Japonais")
                                && it.genres.any { genre -> genre.contains("Anime") }
                    }
                    .forEach { show ->
                        val newSimulcast = configuration!!.newPlatformSimulcast()
                        newSimulcast.uuid = UUID.randomUUID()
                        newSimulcast.name = show.id.toString()
                        newSimulcast.releaseDay = show.availabilityStartTime!!.dayOfWeek.value

                        if (configuration!!.addPlatformSimulcast(newSimulcast)) {
                            saveConfiguration()
                            reset()
                            logger.info("Added new simulcast for show $show")
                        }
                    }
            }
        }

        configuration!!.availableCountries.forEach { countryCode ->
            configuration!!.simulcasts.filter { it.canBeFetch(zonedDateTime) }
                .forEach { simulcast ->
                    list.addAll(
                        getApiContent(
                            PlatformSimulcastFetchCacheKey(
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
                episode.show.title?.let { title -> put(ImageType.TITLE, title) }
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
