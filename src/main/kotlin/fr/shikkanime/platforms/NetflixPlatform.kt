package fr.shikkanime.platforms

import fr.shikkanime.caches.CountryCodeNetflixSimulcastKeyCache
import fr.shikkanime.entities.Anime
import fr.shikkanime.entities.Episode
import fr.shikkanime.entities.enums.EpisodeType
import fr.shikkanime.entities.enums.LangType
import fr.shikkanime.entities.enums.Platform
import fr.shikkanime.platforms.configuration.NetflixConfiguration
import fr.shikkanime.utils.*
import java.io.File
import java.time.LocalTime
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

class NetflixPlatform : AbstractPlatform<NetflixConfiguration, CountryCodeNetflixSimulcastKeyCache, Set<Episode>>() {
    override fun getPlatform(): Platform = Platform.NETF

    override fun getConfigurationClass() = NetflixConfiguration::class.java

    override suspend fun fetchApiContent(
        key: CountryCodeNetflixSimulcastKeyCache,
        zonedDateTime: ZonedDateTime
    ): Set<Episode> {
        val id = key.netflixSimulcast.name
        val season = key.netflixSimulcast.season
        val releaseDateTimeUTC = zonedDateTime.withUTC()
            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) + "T${key.netflixSimulcast.releaseTime}Z"
        val releaseDateTime = ZonedDateTime.parse(releaseDateTimeUTC)

        val document =
            HttpRequest().use { it.getBrowser("https://www.netflix.com/${key.countryCode.name.lowercase()}/title/$id") }
        val animeName = document.selectFirst(".title-title")?.text() ?: return emptySet()
        val animeBanner =
            document.selectXpath("//*[@id=\"section-hero\"]/div[1]/div[2]/picture/source[2]").attr("srcset")
                .substringBefore("?")
        val animeDescription = document.selectFirst(".title-info-synopsis")?.text()
        val episodes = document.selectFirst("ol.episodes-container")?.select("li.episode") ?: emptySet()

        return episodes.mapNotNull { episode ->
            val episodeTitleAndNumber = episode.selectFirst(".episode-title")?.text()
            val episodeTitle = episodeTitleAndNumber?.substringAfter(".")
            val episodeNumber = episodeTitleAndNumber?.substringBefore(".")?.toIntOrNull() ?: -1
            val duration = episode.selectFirst(".episode-runtime")?.text()
            val durationInSeconds = duration?.substringBefore(" ")?.trim()?.toLongOrNull()?.times(60) ?: -1
            val image = episode.selectFirst(".episode-thumbnail-image")?.attr("src") ?: return@mapNotNull null
            val imageWithoutParams = image.substringBefore("?")
            val episodeDescription = episode.selectFirst(".epsiode-synopsis")?.text()
            val computedId = EncryptionManager.toSHA512("$id-${season}-$episodeNumber").substring(0..<8)

            Episode(
                platform = getPlatform(),
                anime = Anime(
                    countryCode = key.countryCode,
                    name = animeName,
                    releaseDateTime = releaseDateTime,
                    image = key.netflixSimulcast.image,
                    banner = animeBanner,
                    description = animeDescription,
                    slug = StringUtils.toSlug(StringUtils.getShortName(animeName)),
                ),
                episodeType = EpisodeType.EPISODE,
                langType = LangType.SUBTITLES,
                audioLocale = "ja-JP",
                hash = StringUtils.getHash(key.countryCode, getPlatform(), computedId, LangType.SUBTITLES),
                releaseDateTime = releaseDateTime,
                season = season,
                number = episodeNumber,
                title = episodeTitle,
                url = "https://www.netflix.com/${key.countryCode.name.lowercase()}/title/$computedId",
                image = imageWithoutParams,
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
                    val api = getApiContent(CountryCodeNetflixSimulcastKeyCache(countryCode, simulcast), zonedDateTime)
                    list.addAll(api)
                }
        }

        return list
    }
}