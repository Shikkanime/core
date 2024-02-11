package fr.shikkanime

import fr.shikkanime.entities.Config
import fr.shikkanime.entities.Episode
import fr.shikkanime.entities.enums.ConfigPropertyKey
import fr.shikkanime.entities.enums.CountryCode
import fr.shikkanime.platforms.AnimationDigitalNetworkPlatform
import fr.shikkanime.platforms.CrunchyrollPlatform
import fr.shikkanime.services.ConfigService
import fr.shikkanime.services.EpisodeService
import fr.shikkanime.utils.Constant
import fr.shikkanime.utils.HttpRequest
import fr.shikkanime.utils.ObjectParser.getAsString
import fr.shikkanime.wrappers.CrunchyrollWrapper
import kotlinx.coroutines.runBlocking
import java.time.LocalDate
import java.time.Period
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

private const val DD_MM_YYYY = "dd/MM/yyyy"
private val ofPattern = DateTimeFormatter.ofPattern(DD_MM_YYYY)

fun main() {
    val httpRequest = HttpRequest()

    println("Enter the from date you want to check ($DD_MM_YYYY):")
    val checkFromDate = readlnOrNull() ?: return
    val start = System.currentTimeMillis()

    val fromDate = try {
        LocalDate.parse(checkFromDate, ofPattern)
    } catch (e: Exception) {
        println("Invalid date (${e.message})")
        return
    }

    println("Enter the to date you want to check ($DD_MM_YYYY):")
    val checkToDate = readlnOrNull() ?: return

    val toDate = try {
        LocalDate.parse(checkToDate, ofPattern)
    } catch (e: Exception) {
        println("Invalid date (${e.message})")
        return
    }

    if (ChronoUnit.DAYS.between(fromDate, toDate).toInt() <= 0) {
        println("Invalid date range")
        return
    }

    val dates = fromDate.datesUntil(toDate.plusDays(1), Period.ofDays(1)).toList()
        .map { ZonedDateTime.of(it.atTime(23, 59, 59), Constant.utcZoneId) }
        .sorted()

    val simulcasts = dates.map {
        "${Constant.seasons[(it.monthValue - 1) / 3]}-${it.year}".lowercase().replace("autumn", "fall")
    }.toSet()

    println("Checking ${dates.size} dates...")
    println("Simulcasts: $simulcasts")

    val episodes = mutableListOf<Episode>()
    val configService = Constant.injector.getInstance(ConfigService::class.java)

    configService.findByName(ConfigPropertyKey.USE_CRUNCHYROLL_API.key)?.let {
        configService.delete(it)
    }

    configService.save(Config(propertyKey = ConfigPropertyKey.USE_CRUNCHYROLL_API.key, propertyValue = "true"))
    val episodeService = Constant.injector.getInstance(EpisodeService::class.java)
    val adnPlatform = Constant.injector.getInstance(AnimationDigitalNetworkPlatform::class.java)
    val crunchyrollPlatform = Constant.injector.getInstance(CrunchyrollPlatform::class.java)

//    dates.forEach { date ->
//        runBlocking { adnPlatform.fetchApiContent(CountryCode.FR, date) }.forEach { episodeJson ->
//            try {
//                episodes.addAll(adnPlatform.convertEpisode(
//                    CountryCode.FR,
//                    episodeJson.asJsonObject,
//                    date
//                ))
//            } catch (e: Exception) {
//                e.printStackTrace()
//            }
//        }
//    }

    val minDate = dates.minOrNull()!!
    val maxDate = dates.maxOrNull()!!
    val accessToken = runBlocking { CrunchyrollWrapper.getAnonymousAccessToken() }
    var page = 1
    val fetch = 1000

    val series = simulcasts.flatMap { simulcastId ->
        runBlocking {
            CrunchyrollWrapper.getBrowse(
                CountryCode.FR.locale,
                accessToken,
                sortBy = CrunchyrollWrapper.SortType.POPULARITY,
                type = CrunchyrollWrapper.MediaType.SERIES,
                100,
                simulcast = simulcastId
            )
        }
    }.map { jsonObject -> jsonObject.getAsString("title")!!.lowercase() }.toSet()
    println("Simulcasts: $series")

    crunchyrollPlatform.simulcasts[CountryCode.FR] = series

    while (true) {
        val fetchApi = runBlocking { CrunchyrollWrapper.getBrowse(CountryCode.FR.locale, accessToken, size = fetch, start = (page - 1) * fetch) }.toMutableList()

        val episodeDates = fetchApi
            .map { episodeJson ->
                val episodeMetadata = episodeJson.getAsJsonObject("episode_metadata")
                episodeJson to requireNotNull(episodeMetadata.getAsString("premium_available_date")?.let { ZonedDateTime.parse(it) }) { "Release date is null" }
            }.toMutableList()

        val lastDate = episodeDates.maxByOrNull { it.second }!!.second
        val firstDate = episodeDates.minByOrNull { it.second }!!.second
        episodeDates.removeIf { it.second !in minDate..maxDate }

        episodeDates.forEach { (episodeJson, _) ->
            try {
                episodes.add(crunchyrollPlatform.convertJsonEpisode(
                    CountryCode.FR,
                    episodeJson
                ))
            } catch (_: Exception) {

            }
        }

        if (lastDate.isBefore(minDate) || firstDate.isAfter(maxDate)) {
            break
        }

        page++
    }

    httpRequest.close()

    episodes.forEach { episode ->
        episodeService.save(episode)
    }

    println("Take ${(System.currentTimeMillis() - start) / 1000}s to check ${dates.size} dates")
}