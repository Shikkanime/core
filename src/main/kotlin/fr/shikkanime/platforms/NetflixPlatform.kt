package fr.shikkanime.platforms

import fr.shikkanime.caches.CountryCodeNetflixSimulcastKeyCache
import fr.shikkanime.entities.enums.CountryCode
import fr.shikkanime.entities.enums.EpisodeType
import fr.shikkanime.entities.enums.Platform
import fr.shikkanime.platforms.configuration.NetflixConfiguration
import fr.shikkanime.utils.isEqualOrAfter
import fr.shikkanime.utils.withUTC
import fr.shikkanime.wrappers.factories.AbstractNetflixWrapper
import fr.shikkanime.wrappers.impl.NetflixWrapper
import java.io.File
import java.time.LocalTime
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

class NetflixPlatform :
    AbstractPlatform<NetflixConfiguration, CountryCodeNetflixSimulcastKeyCache, List<AbstractPlatform.Episode>?>() {

    override fun getPlatform(): Platform = Platform.NETF

    override fun getConfigurationClass() = NetflixConfiguration::class.java

    override suspend fun fetchApiContent(
        key: CountryCodeNetflixSimulcastKeyCache,
        zonedDateTime: ZonedDateTime
    ): List<Episode>? {
        val releaseDateTime = ZonedDateTime.parse(zonedDateTime.withUTC().format(DateTimeFormatter.ISO_LOCAL_DATE) + "T${key.netflixSimulcast.releaseTime}Z")
        val episodes = NetflixWrapper.getShowVideos(key.countryCode,  key.netflixSimulcast.name, key.netflixSimulcast.seasonName, key.netflixSimulcast.season) ?: return null

        return key.netflixSimulcast.audioLocales.flatMap { audioLocale ->
            episodes.map {
                convertEpisode(
                    key.countryCode,
                    key.netflixSimulcast.image,
                    it,
                    releaseDateTime,
                    key.netflixSimulcast.episodeType,
                    audioLocale
                )
            }
        }
    }

    override fun fetchEpisodes(zonedDateTime: ZonedDateTime, bypassFileContent: File?): List<Episode> {
        val list = mutableListOf<Episode>()

        configuration!!.availableCountries.forEach { countryCode ->
            configuration!!.simulcasts.filter {
                it.releaseDay == zonedDateTime.dayOfWeek.value && zonedDateTime.toLocalTime()
                    .isEqualOrAfter(LocalTime.parse(it.releaseTime))
            }
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