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
import java.time.temporal.ChronoUnit

class NetflixPlatform :
    AbstractPlatform<NetflixConfiguration, CountryCodeNetflixSimulcastKeyCache, List<AbstractPlatform.Episode>?>() {

    override fun getPlatform(): Platform = Platform.NETF

    override fun getConfigurationClass() = NetflixConfiguration::class.java

    override suspend fun fetchApiContent(
        key: CountryCodeNetflixSimulcastKeyCache,
        zonedDateTime: ZonedDateTime
    ): List<Episode>? {
        val episodes = NetflixWrapper.getShowVideos(
            key.countryCode, 
            key.netflixSimulcast.name, 
            key.netflixSimulcast.seasonName, 
            key.netflixSimulcast.season
        ) ?: return null

        val result = mutableListOf<Episode>()
        
        episodes.forEach { episode ->
            processEpisodeForAllAudioLocales(episode, key, zonedDateTime, result)
        }
        
        return result
    }
    
    private fun processEpisodeForAllAudioLocales(
        episode: AbstractNetflixWrapper.Episode,
        key: CountryCodeNetflixSimulcastKeyCache,
        zonedDateTime: ZonedDateTime,
        result: MutableList<Episode>
    ) {
        key.netflixSimulcast.audioLocales.forEach { audioLocale ->
            val adjustedReleaseDateTime = calculateReleaseDateTime(audioLocale, key.netflixSimulcast.audioLocaleDelays, zonedDateTime)
            
            addEpisodeToResults(
                episode,
                key.countryCode,
                key.netflixSimulcast.image,
                adjustedReleaseDateTime,
                key.netflixSimulcast.episodeType,
                audioLocale,
                result
            )
        }
    }
    
    private fun calculateReleaseDateTime(
        audioLocale: String,
        audioLocaleDelays: Map<String, Int>,
        baseDateTime: ZonedDateTime
    ): ZonedDateTime {
        val delayInWeeks = audioLocaleDelays[audioLocale] ?: 0
        
        return if (delayInWeeks > 0) {
            // If there's a delay for this locale, adjust the release date accordingly
            baseDateTime.plus(delayInWeeks.toLong() * 7, ChronoUnit.DAYS)
        } else {
            // No delay, use original release date
            baseDateTime
        }
    }
    
    private fun addEpisodeToResults(
        episode: AbstractNetflixWrapper.Episode,
        countryCode: CountryCode,
        showImage: String,
        releaseDateTime: ZonedDateTime,
        episodeType: EpisodeType,
        audioLocale: String,
        results: MutableList<Episode>
    ) {
        results.add(
            convertEpisode(
                countryCode,
                showImage,
                episode,
                releaseDateTime,
                episodeType,
                audioLocale
            )
        )
    }

    override fun fetchEpisodes(zonedDateTime: ZonedDateTime, bypassFileContent: File?): List<Episode> {
        val list = mutableListOf<Episode>()

        configuration!!.availableCountries.forEach { countryCode ->
            configuration!!.simulcasts.forEach { simulcast ->
                processSimulcast(countryCode, simulcast, zonedDateTime, list)
            }
        }

        return list
    }
    
    private fun processSimulcast(
        countryCode: CountryCode,
        simulcast: NetflixConfiguration.NetflixSimulcastDay,
        zonedDateTime: ZonedDateTime,
        resultList: MutableList<Episode>
    ) {
        // Case 1: Regular release for today
        if (isReleaseDay(simulcast.releaseDay, zonedDateTime)) {
            fetchAndAddEpisodes(countryCode, simulcast, zonedDateTime, resultList)
            return
        }
        
        // Case 2: Check for delayed releases
        if (hasDelayedReleaseToday(simulcast, zonedDateTime)) {
            // Use a fixed delay for fetching content, actual delay applied in fetchApiContent
            val adjustedDate = zonedDateTime.minus(1, ChronoUnit.DAYS)
            fetchAndAddEpisodes(countryCode, simulcast, adjustedDate, resultList)
        }
    }
    
    private fun isReleaseDay(releaseDay: Int, date: ZonedDateTime): Boolean {
        return releaseDay == 0 || releaseDay == date.dayOfWeek.value
    }
    
    private fun hasDelayedReleaseToday(
        simulcast: NetflixConfiguration.NetflixSimulcastDay,
        zonedDateTime: ZonedDateTime
    ): Boolean {
        return simulcast.audioLocaleDelays.any { (locale, weeks) ->
            isDelayedEpisodeReleasingToday(simulcast, locale, weeks, zonedDateTime)
        }
    }
    
    private fun isDelayedEpisodeReleasingToday(
        simulcast: NetflixConfiguration.NetflixSimulcastDay,
        locale: String,
        weeks: Int,
        zonedDateTime: ZonedDateTime
    ): Boolean {
        if (weeks <= 0 || !simulcast.audioLocales.contains(locale)) {
            return false
        }
        
        // Calculate the original release date by going back in time by the delay period
        val originalReleaseDate = zonedDateTime.minus(weeks.toLong() * 7, ChronoUnit.DAYS)
        
        // Check if the original release date was on the configured release day
        return originalReleaseDate.dayOfWeek.value == simulcast.releaseDay
    }
    
    private fun fetchAndAddEpisodes(
        countryCode: CountryCode,
        simulcast: NetflixConfiguration.NetflixSimulcastDay,
        date: ZonedDateTime,
        resultList: MutableList<Episode>
    ) {
        getApiContent(CountryCodeNetflixSimulcastKeyCache(countryCode, simulcast), date)
            ?.let { resultList.addAll(it) }
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