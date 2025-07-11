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

class NetflixPlatform : AbstractPlatform<NetflixConfiguration, CountryCodeNetflixSimulcastKeyCache, List<AbstractPlatform.Episode>>() {
    @Inject private lateinit var episodeVariantCacheService: EpisodeVariantCacheService

    override fun getPlatform(): Platform = Platform.NETF

    override fun getConfigurationClass() = NetflixConfiguration::class.java

    override suspend fun fetchApiContent(
        key: CountryCodeNetflixSimulcastKeyCache,
        zonedDateTime: ZonedDateTime
    ): List<Episode> {
        val episodes = NetflixWrapper.getEpisodesByShowId(key.countryCode.locale, key.netflixSimulcast.name.toInt())
        val identifiers = mutableMapOf<Int, ZonedDateTime>()

        return episodes.flatMap { video ->
            key.netflixSimulcast.audioLocales.mapNotNull { audioLocale ->
                val episode = runCatching {
                    convertEpisode(
                        key.countryCode,
                        key.netflixSimulcast.image,
                        video,
                        key.netflixSimulcast.episodeType,
                        audioLocale
                    )
                }.getOrNull() ?: return@mapNotNull null

                val releaseDateTime = identifiers.computeIfAbsent(video.id) {
                    episodeVariantCacheService.findByIdentifier(episode.getIdentifier())
                        ?.releaseDateTime ?: episode.releaseDateTime
                }

                key.netflixSimulcast.audioLocaleDelays[audioLocale]?.let { delay ->
                    episode.releaseDateTime = releaseDateTime.plusWeeks(delay)
                }

                episode
            }
        }
    }

    override fun fetchEpisodes(zonedDateTime: ZonedDateTime, bypassFileContent: File?): List<Episode> {
        val list = mutableListOf<Episode>()

        configuration!!.availableCountries.forEach { countryCode ->
            configuration!!.simulcasts.filter { it.releaseDay == 0 || it.releaseDay == zonedDateTime.dayOfWeek.value }
                .forEach { simulcast ->
                    list.addAll(getApiContent(CountryCodeNetflixSimulcastKeyCache(countryCode, simulcast), zonedDateTime))
                }
        }

        return list
    }

    fun convertEpisode(
        countryCode: CountryCode,
        showImage: String,
        episode: AbstractNetflixWrapper.Episode,
        episodeType: EpisodeType,
        audioLocale: String,
    ) = Episode(
        countryCode = countryCode,
        animeId = episode.show.id.toString(),
        anime = episode.show.name,
        animeImage = episode.show.thumbnail ?: showImage,
        animeBanner = episode.show.banner,
        animeDescription = episode.show.description,
        releaseDateTime = requireNotNull(episode.releaseDateTime) { "Release date is null" },
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
        id = episode.id.toString(),
        url = episode.url,
        uncensored = false,
        original = true,
    )
}