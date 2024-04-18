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
    AbstractPlatform<PrimeVideoConfiguration, CountryCodePrimeVideoSimulcastKeyCache, Set<AbstractPlatform.Episode>>() {
    override fun getPlatform(): Platform = Platform.PRIM

    override fun getConfigurationClass() = PrimeVideoConfiguration::class.java

    override suspend fun fetchApiContent(
        key: CountryCodePrimeVideoSimulcastKeyCache,
        zonedDateTime: ZonedDateTime
    ): Set<Episode> {
        val id = key.primeVideoSimulcast.name
        val releaseDateTimeUTC = zonedDateTime.withUTC()
            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) + "T${key.primeVideoSimulcast.releaseTime}Z"
        val releaseDateTime = ZonedDateTime.parse(releaseDateTimeUTC)

        val episodes = PrimeVideoWrapper.getShowVideos(key.countryCode.name, key.countryCode.locale, id)

        return episodes.map {
            val animeName = requireNotNull(it.getAsJsonObject("show").getAsString("name")) { "Name is null" }
            val animeBanner = requireNotNull(it.getAsJsonObject("show").getAsString("banner")) { "Banner is null" }
            val image = requireNotNull(it.getAsString("image")) { "Image is null" }
            val computedId = requireNotNull(it.getAsJsonObject("show").getAsString("banner")) { "Id is null" }
            val url = requireNotNull(it.getAsJsonObject("show").getAsString("url")) { "Url is null" }

            Episode(
                countryCode = key.countryCode,
                anime = animeName,
                animeImage = key.primeVideoSimulcast.image,
                animeBanner = animeBanner,
                animeDescription = it.getAsJsonObject("show").getAsString("description"),
                releaseDateTime = releaseDateTime,
                episodeType = EpisodeType.EPISODE,
                season = it.getAsInt("season")!!,
                number = it.getAsInt("number")!!,
                duration = it.getAsInt("duration")?.toLong() ?: -1,
                title = it.getAsString("title"),
                description = it.getAsString("description"),
                image = image,
                platform = getPlatform(),
                audioLocale = "ja-JP",
                id = computedId,
                url = url,
                uncensored = false
            )
        }.toSet()
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