package fr.shikkanime.platforms

import fr.shikkanime.caches.CountryCodePrimeVideoSimulcastKeyCache
import fr.shikkanime.entities.Anime
import fr.shikkanime.entities.Episode
import fr.shikkanime.entities.enums.EpisodeType
import fr.shikkanime.entities.enums.LangType
import fr.shikkanime.entities.enums.Platform
import fr.shikkanime.platforms.configuration.PrimeVideoConfiguration
import fr.shikkanime.utils.ObjectParser.getAsInt
import fr.shikkanime.utils.ObjectParser.getAsString
import fr.shikkanime.utils.StringUtils
import fr.shikkanime.utils.isEqualOrAfter
import fr.shikkanime.utils.withUTC
import fr.shikkanime.wrappers.PrimeVideoWrapper
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
        val episodes = PrimeVideoWrapper.getShowVideos(key.countryCode.name, key.countryCode.locale, id)

        return episodes.map {
            val animeName = it.getAsJsonObject("show").getAsString("name")!!

            val computedId = it.getAsString("id")!!
            val audioLocale = "ja-JP"
            val langType = LangType.SUBTITLES
            val (_, hash) = getDeprecatedHashAndHash(key.countryCode, computedId, audioLocale, langType)

            Episode(
                platform = getPlatform(),
                anime = Anime(
                    countryCode = key.countryCode,
                    name = animeName,
                    releaseDateTime = releaseDateTime,
                    image = key.primeVideoSimulcast.image,
                    banner = it.getAsJsonObject("show").getAsString("banner"),
                    description = it.getAsJsonObject("show").getAsString("description"),
                    slug = StringUtils.toSlug(StringUtils.getShortName(animeName)),
                ),
                episodeType = EpisodeType.EPISODE,
                langType = langType,
                audioLocale = audioLocale,
                hash = hash,
                releaseDateTime = releaseDateTime,
                season = it.getAsInt("season")!!,
                number = it.getAsInt("number")!!,
                title = it.getAsString("title"),
                url = it.getAsString("url"),
                image = it.getAsString("image")!!,
                duration = it.getAsInt("duration")?.toLong() ?: -1,
                description = it.getAsString("description"),
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