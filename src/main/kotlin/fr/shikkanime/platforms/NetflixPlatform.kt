package fr.shikkanime.platforms

import com.google.inject.Inject
import fr.shikkanime.caches.CountryCodeNetflixSimulcastKeyCache
import fr.shikkanime.entities.enums.CountryCode
import fr.shikkanime.entities.enums.EpisodeType
import fr.shikkanime.entities.enums.Platform
import fr.shikkanime.platforms.configuration.NetflixConfiguration
import fr.shikkanime.services.caches.EpisodeVariantCacheService
import fr.shikkanime.wrappers.factories.AbstractNetflixWrapper
import fr.shikkanime.wrappers.impl.NetflixWrapper
import java.io.File
import java.time.ZonedDateTime

class NetflixPlatform : AbstractPlatform<NetflixConfiguration, CountryCodeNetflixSimulcastKeyCache, List<AbstractPlatform.Episode>?>() {
    @Inject
    private lateinit var episodeVariantCacheService: EpisodeVariantCacheService

    override fun getPlatform(): Platform = Platform.NETF

    override fun getConfigurationClass() = NetflixConfiguration::class.java

    override suspend fun fetchApiContent(
        key: CountryCodeNetflixSimulcastKeyCache,
        zonedDateTime: ZonedDateTime
    ): List<Episode>? {
        val videos = NetflixWrapper.getShowVideos(
            key.countryCode, key.netflixSimulcast.name,
            key.netflixSimulcast.seasonName, key.netflixSimulcast.season
        ) ?: return null

        // Cache for original release dates per video ID
        val originalReleaseDateCache = mutableMapOf<String, ZonedDateTime>()

        return videos.flatMap { video ->
            key.netflixSimulcast.audioLocales.map { audioLocale ->
                val episode = convertEpisode(
                    key.countryCode,
                    key.netflixSimulcast.image,
                    video,
                    zonedDateTime,
                    key.netflixSimulcast.episodeType,
                    audioLocale
                )

                // Fetch and cache the original release date only once per video ID
                // Uses the identifier from the generated episode for the cache lookup.
                val originalReleaseDateTime = originalReleaseDateCache.getOrPut(video.id) {
                    episodeVariantCacheService.findReleaseDateTimeByIdentifier(episode.getIdentifier()) ?: zonedDateTime
                }

                // Apply delay if delay is defined for this locale
                key.netflixSimulcast.audioLocaleDelays[audioLocale]?.let { delayInWeeks ->
                    episode.releaseDateTime = originalReleaseDateTime.plusWeeks(delayInWeeks)
                }
                // If no delay is applicable, the releaseDateTime set by convertEpisode is used.

                episode
            }
        }
    }

    override fun fetchEpisodes(zonedDateTime: ZonedDateTime, bypassFileContent: File?): List<Episode> {
        val list = mutableListOf<Episode>()

        configuration!!.availableCountries.forEach { countryCode ->
            configuration!!.simulcasts.filter { it.releaseDay == 0 || it.releaseDay == zonedDateTime.dayOfWeek.value }
                .forEach { simulcast ->
                    getApiContent(CountryCodeNetflixSimulcastKeyCache(countryCode, simulcast), zonedDateTime)
                        ?.let { list.addAll(it) }
                }
        }

        return list
    }

    fun getShowId(url: String) =
        "https://www\\.netflix\\.com/[a-z]{2}/title/([0-9]{8})".toRegex().find(url)?.groupValues?.get(1)

    fun convertEpisode(
        countryCode: CountryCode,
        showImage: String,
        episode: AbstractNetflixWrapper.Episode,
        zonedDateTime: ZonedDateTime,
        episodeType: EpisodeType,
        audioLocale: String,
    ) = Episode(
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
        audioLocale = audioLocale,
        id = episode.id,
        url = episode.url,
        uncensored = false,
        original = true,
    )
}