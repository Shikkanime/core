package fr.shikkanime.platforms

import fr.shikkanime.caches.CountryCodeNetflixSimulcastKeyCache
import fr.shikkanime.entities.enums.CountryCode
import fr.shikkanime.entities.enums.EpisodeType
import fr.shikkanime.entities.enums.Platform
import fr.shikkanime.platforms.configuration.NetflixConfiguration
import fr.shikkanime.wrappers.factories.AbstractNetflixWrapper
import fr.shikkanime.wrappers.impl.NetflixWrapper
import java.io.File
import java.time.ZonedDateTime
import java.util.logging.Level

class NetflixPlatform : AbstractPlatform<NetflixConfiguration, CountryCodeNetflixSimulcastKeyCache, List<AbstractPlatform.Episode>>() {
    override fun getPlatform(): Platform = Platform.NETF

    override fun getConfigurationClass() = NetflixConfiguration::class.java

    override suspend fun fetchApiContent(
        key: CountryCodeNetflixSimulcastKeyCache,
        zonedDateTime: ZonedDateTime
    ): List<Episode> {
        val episodes = NetflixWrapper.getEpisodesByShowId(key.countryCode.locale, key.netflixSimulcast.name.toInt())

        return episodes.flatMap { video ->
            key.netflixSimulcast.audioLocales.mapNotNull { audioLocale ->
                val episode = try {
                    convertEpisode(
                        key.countryCode,
                        key.netflixSimulcast.image,
                        video,
                        key.netflixSimulcast.episodeType,
                        audioLocale
                    )
                } catch (e: Exception) {
                    logger.log(Level.SEVERE, "Error on converting episode", e)
                    null
                } ?: return@mapNotNull null

                // Apply delay if delay is defined for this locale
                key.netflixSimulcast.audioLocaleDelays[audioLocale]?.let { delayInWeeks ->
                    episode.releaseDateTime = episode.releaseDateTime.plusWeeks(delayInWeeks)
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
        animeImage = showImage,
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