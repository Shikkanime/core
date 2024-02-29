package fr.shikkanime.platforms

import fr.shikkanime.caches.CountryCodePrimeVideoSimulcastKeyCache
import fr.shikkanime.entities.Anime
import fr.shikkanime.entities.Episode
import fr.shikkanime.entities.enums.EpisodeType
import fr.shikkanime.entities.enums.LangType
import fr.shikkanime.entities.enums.Platform
import fr.shikkanime.platforms.configuration.PrimeVideoConfiguration
import fr.shikkanime.utils.*
import java.io.File
import java.time.LocalTime
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

class PrimeVideoPlatform :
    AbstractPlatform<PrimeVideoConfiguration, CountryCodePrimeVideoSimulcastKeyCache, Set<Episode>>() {
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

        val document =
            HttpRequest().use { it.getBrowser("https://www.primevideo.com/-/${key.countryCode.name.lowercase()}/detail/$id?language=${key.countryCode.locale}") }
        val animeName = document.selectFirst("h1[data-automation-id=\"title\"]")?.text() ?: return emptySet()
        val animeBanner = document.selectFirst("img[data-testid=\"base-image\"]")?.attr("src")
        val animeDescription = document.selectFirst(".dv-dp-node-synopsis")?.text()
        val domEpisodes = document.selectFirst("ol.GG33WY")?.select("li.c5qQpO") ?: emptySet()

        return domEpisodes.mapNotNull { domEpisode ->
            val episodeTitle = domEpisode.selectFirst("span.P1uAb6")?.text()
            val episodeSeasonAndNumber = domEpisode.selectFirst("span.izvPPq > span")?.text()
            val season = episodeSeasonAndNumber?.substringAfter("S. ")?.substringBefore(" ")?.trim()?.toIntOrNull() ?: 1
            val episodeNumber =
                episodeSeasonAndNumber?.substringAfter("ÉP. ")?.substringBefore(" ")?.trim()?.toIntOrNull() ?: -1
            val duration = domEpisode.selectFirst("div[data-testid=\"episode-runtime\"]")?.text()
            val durationInSeconds = duration?.substringBefore("min")?.trim()?.toLongOrNull()?.times(60) ?: -1
            val image = domEpisode.selectFirst("img[data-testid=\"base-image\"]")?.attr("src") ?: return@mapNotNull null
            val episodeDescription = domEpisode.selectFirst("div[dir=\"auto\"]")?.text()

            Episode(
                platform = getPlatform(),
                anime = Anime(
                    countryCode = key.countryCode,
                    name = animeName,
                    releaseDateTime = releaseDateTime,
                    image = key.primeVideoSimulcast.image,
                    banner = animeBanner,
                    description = animeDescription,
                    slug = StringUtils.toSlug(StringUtils.getShortName(animeName)),
                ),
                episodeType = EpisodeType.EPISODE,
                langType = LangType.SUBTITLES,
                hash = "${key.countryCode}-${getPlatform()}-${
                    EncryptionManager.toSHA512("$id-${season}-$episodeNumber").substring(0..<8)
                }-${LangType.SUBTITLES}",
                releaseDateTime = releaseDateTime,
                season = season,
                number = episodeNumber,
                title = episodeTitle,
                url = "https://www.primevideo.com/-/${key.countryCode.name.lowercase()}/detail/$id",
                image = image,
                duration = durationInSeconds,
                description = episodeDescription,
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