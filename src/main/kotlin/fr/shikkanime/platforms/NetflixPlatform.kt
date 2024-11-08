package fr.shikkanime.platforms

import fr.shikkanime.caches.CountryCodeNetflixSimulcastKeyCache
import fr.shikkanime.entities.enums.CountryCode
import fr.shikkanime.entities.enums.Platform
import fr.shikkanime.platforms.configuration.NetflixConfiguration
import fr.shikkanime.utils.*
import org.jsoup.nodes.Document
import java.io.File
import java.time.LocalTime
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

class NetflixPlatform :
    AbstractPlatform<NetflixConfiguration, CountryCodeNetflixSimulcastKeyCache, Set<AbstractPlatform.Episode>?>() {
    private val maxRetry = 10

    override fun getPlatform(): Platform = Platform.NETF

    override fun getConfigurationClass() = NetflixConfiguration::class.java

    private fun loadContent(id: String, countryCode: CountryCode, i: Int = 0): Document? {
        if (i >= maxRetry) {
            logger.severe("Failed to fetch Netflix page for $id in ${countryCode.name}")
            return null
        }

        val document =
            HttpRequest(countryCode).use { it.getBrowser("https://www.netflix.com/${countryCode.name.lowercase()}/title/$id") }

        if (document.getElementsByTag("html").attr("lang") != countryCode.name.lowercase()) {
            logger.warning("Failed to fetch Netflix page for $id in ${countryCode.name}, retrying... ($i/${maxRetry})")
            return loadContent(id, countryCode, i + 1)
        }

        // Is new design?
        if (document.selectXpath("//*[@id=\"appMountPoint\"]/div/div[2]/div/header").isNotEmpty()) {
            logger.warning("New Netflix design detected, trying to get the old design... ($i/${maxRetry})")
            return loadContent(id, countryCode, i + 1)
        }

        return document
    }

    override suspend fun fetchApiContent(
        key: CountryCodeNetflixSimulcastKeyCache,
        zonedDateTime: ZonedDateTime
    ): Set<Episode>? {
        val id = key.netflixSimulcast.name
        val season = key.netflixSimulcast.season
        val releaseDateTimeUTC = zonedDateTime.withUTC()
            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) + "T${key.netflixSimulcast.releaseTime}Z"
        val releaseDateTime = ZonedDateTime.parse(releaseDateTimeUTC)
        val document = loadContent(id, key.countryCode) ?: return null
        val animeName = document.selectFirst(".title-title")?.text() ?: return emptySet()
        val animeBanner =
            document.selectXpath("//*[@id=\"section-hero\"]/div[1]/div[2]/picture/source[2]").attr("srcset")
                .substringBefore("?")
        val animeDescription = document.selectFirst(".title-info-synopsis")?.text()

        val seasonsOption = document.select("#season-selector-container").select("option")
        val seasons = document.select(".season")

        // Group episodes by season
        val episodesBySeason = seasons.mapIndexed { index, seasonElement ->
            val seasonName = seasonsOption.getOrNull(index)?.text()
            val seasonEpisodes = seasonElement.select("li.episode")
            seasonName to seasonEpisodes
        }.toMap()

        var useAllSeasons = false

        val episodes = episodesBySeason[key.netflixSimulcast.seasonName] ?: run {
            logger.warning("Season name is blank or not found, fetching all episodes from all seasons and ignoring season number")
            useAllSeasons = true
            document.select("li.episode")
        }

        return episodes.mapIndexedNotNull { index, episode ->
            val episodeTitleAndNumber = episode.selectFirst(".episode-title")?.text()
            val episodeTitle = episodeTitleAndNumber?.substringAfter(".")
            val episodeNumber = episodeTitleAndNumber?.substringBefore(".")?.toIntOrNull() ?: (index + 1)
            val duration = episode.selectFirst(".episode-runtime")?.text()
            val durationInSeconds = duration?.substringBefore(" ")?.trim()?.toLongOrNull()?.times(60) ?: -1
            val image = episode.selectFirst(".episode-thumbnail-image")?.attr("src") ?: return@mapIndexedNotNull null
            val imageWithoutParams = image.substringBefore("?")
            val episodeDescription = episode.selectFirst(".epsiode-synopsis")?.text()

            val episodeSeason = if (useAllSeasons) seasons.indexOf(episode.closest(".season")!!) + 1 else season
            val computedId = EncryptionManager.toSHA512("$id-$episodeSeason-$episodeNumber").substring(0..<8)

            Episode(
                countryCode = key.countryCode,
                animeId = id,
                anime = animeName,
                animeImage = key.netflixSimulcast.image,
                animeBanner = animeBanner,
                animeDescription = animeDescription.normalize(),
                releaseDateTime = releaseDateTime,
                episodeType = key.netflixSimulcast.episodeType,
                season = episodeSeason,
                number = episodeNumber,
                duration = durationInSeconds,
                title = episodeTitle.normalize(),
                description = episodeDescription.normalize(),
                image = imageWithoutParams,
                platform = getPlatform(),
                audioLocale = "ja-JP",
                id = computedId,
                url = "https://www.netflix.com/${key.countryCode.name.lowercase()}/title/$id",
                uncensored = false,
                original = true,
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
                    getApiContent(CountryCodeNetflixSimulcastKeyCache(countryCode, simulcast), zonedDateTime)
                        ?.let { list.addAll(it) }
                }
        }

        return list
    }
}