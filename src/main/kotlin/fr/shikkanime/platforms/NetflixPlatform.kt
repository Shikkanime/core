package fr.shikkanime.platforms

import fr.shikkanime.caches.CountryCodeNetflixSimulcastKeyCache
import fr.shikkanime.entities.enums.Platform
import fr.shikkanime.platforms.configuration.NetflixConfiguration
import fr.shikkanime.utils.*
import fr.shikkanime.utils.ObjectParser.getAsInt
import fr.shikkanime.utils.ObjectParser.getAsString
import fr.shikkanime.wrappers.NetflixWrapper
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
        val id = key.netflixSimulcast.name
        val episodes = NetflixWrapper.getShowVideos(key.countryCode, id, key.netflixSimulcast.seasonName, key.netflixSimulcast.season) ?: return null

        return episodes.map {
            Episode(
                countryCode = key.countryCode,
                animeId = id,
                anime = requireNotNull(it.getAsJsonObject("show").getAsString("name")) { "Name is null" },
                animeImage = key.netflixSimulcast.image,
                animeBanner = requireNotNull(it.getAsJsonObject("show").getAsString("banner")) { "Banner is null" },
                animeDescription = it.getAsJsonObject("show").getAsString("description"),
                releaseDateTime = ZonedDateTime.parse(zonedDateTime.withUTC().format(DateTimeFormatter.ISO_LOCAL_DATE) + "T${key.netflixSimulcast.releaseTime}Z"),
                episodeType = key.netflixSimulcast.episodeType,
                season = it.getAsInt("season")!!,
                number = it.getAsInt("number")!!,
                duration = it.getAsInt("duration")?.toLong() ?: -1,
                title = it.getAsString("title"),
                description = it.getAsString("description"),
                image = requireNotNull(it.getAsString("image")) { "Image is null" },
                platform = getPlatform(),
                audioLocale = "ja-JP",
                id = requireNotNull(it.getAsString("id")) { "Id is null" },
                url = requireNotNull(it.getAsString("url")) { "Url is null" },
                uncensored = false,
                original = true,
            )
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
}