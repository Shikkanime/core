package fr.shikkanime.platforms

import fr.shikkanime.caches.CountryCodePrimeVideoSimulcastKeyCache
import fr.shikkanime.entities.enums.EpisodeType
import fr.shikkanime.entities.enums.Platform
import fr.shikkanime.platforms.configuration.PrimeVideoConfiguration
import fr.shikkanime.utils.ObjectParser.getAsInt
import fr.shikkanime.utils.ObjectParser.getAsString
import fr.shikkanime.utils.isEqualOrAfter
import fr.shikkanime.utils.withUTC
import fr.shikkanime.wrappers.PrimeVideoWrapper
import java.io.File
import java.time.LocalTime
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

class PrimeVideoPlatform :
    AbstractPlatform<PrimeVideoConfiguration, CountryCodePrimeVideoSimulcastKeyCache, List<AbstractPlatform.Episode>>() {
    override fun getPlatform(): Platform = Platform.PRIM

    override fun getConfigurationClass() = PrimeVideoConfiguration::class.java

    override suspend fun fetchApiContent(
        key: CountryCodePrimeVideoSimulcastKeyCache,
        zonedDateTime: ZonedDateTime
    ): List<Episode> {
        val id = key.primeVideoSimulcast.name
        val episodes = PrimeVideoWrapper.getShowVideos(key.countryCode, key.countryCode.locale, id)

        return episodes.map {
            Episode(
                countryCode = key.countryCode,
                animeId = id,
                anime = requireNotNull(it.getAsJsonObject("show").getAsString("name")) { "Name is null" },
                animeImage = key.primeVideoSimulcast.image,
                animeBanner = requireNotNull(it.getAsJsonObject("show").getAsString("banner")) { "Banner is null" },
                animeDescription = it.getAsJsonObject("show").getAsString("description"),
                releaseDateTime = ZonedDateTime.parse(zonedDateTime.withUTC().format(DateTimeFormatter.ISO_LOCAL_DATE) + "T${key.primeVideoSimulcast.releaseTime}Z"),
                episodeType = EpisodeType.EPISODE,
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
                    val api =
                        getApiContent(CountryCodePrimeVideoSimulcastKeyCache(countryCode, simulcast), zonedDateTime)
                    list.addAll(api)
                }
        }

        return list
    }
}