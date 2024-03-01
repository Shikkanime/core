package fr.shikkanime

import fr.shikkanime.caches.CountryCodeAnimeIdKeyCache
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
import fr.shikkanime.utils.ObjectParser.getAsInt
import fr.shikkanime.utils.ObjectParser.getAsString
import fr.shikkanime.wrappers.CrunchyrollWrapper
import kotlinx.coroutines.runBlocking
import java.time.LocalDate
import java.time.Period
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

    val dates = fromDate.datesUntil(toDate.plusDays(1), Period.ofDays(1)).toList().sorted()

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

    val accessToken = runBlocking { CrunchyrollWrapper.getAnonymousAccessToken() }
    val cms = runBlocking { CrunchyrollWrapper.getCMS(accessToken) }

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
    }

    val titles = series.map { jsonObject -> jsonObject.getAsString("title")!!.lowercase() }.toSet()
    val ids = series.map { jsonObject -> jsonObject.getAsString("id")!! }.toSet()
    println("Simulcasts: $titles")

    crunchyrollPlatform.simulcasts.set(CountryCode.FR, titles)

    series.forEach {
        val postersTall = it.getAsJsonObject("images").getAsJsonArray("poster_tall")[0].asJsonArray
        val postersWide = it.getAsJsonObject("images").getAsJsonArray("poster_wide")[0].asJsonArray
        val image =
            postersTall?.maxByOrNull { poster -> poster.asJsonObject.getAsInt("width")!! }?.asJsonObject?.getAsString("source")!!
        val banner =
            postersWide?.maxByOrNull { poster -> poster.asJsonObject.getAsInt("width")!! }?.asJsonObject?.getAsString("source")!!
        val description = it.getAsString("description")

        crunchyrollPlatform.animeInfoCache.set(
            CountryCodeAnimeIdKeyCache(CountryCode.FR, it.getAsString("id")!!),
            CrunchyrollPlatform.CrunchyrollAnimeContent(image = image, banner = banner, description = description)
        )
    }

    val episodeIds = ids.parallelStream().map { seriesId ->
        runBlocking { CrunchyrollWrapper.getSeasons(CountryCode.FR.locale, accessToken, cms, seriesId) }
            .filter { jsonObject ->
                jsonObject.getAsJsonArray("subtitle_locales").map { it.asString }.contains(CountryCode.FR.locale)
            }
            .map { jsonObject -> jsonObject.getAsString("id")!! }
            .flatMap { id ->
                runBlocking {
                    CrunchyrollWrapper.getEpisodes(
                        CountryCode.FR.locale,
                        accessToken,
                        cms,
                        id
                    )
                }
            }
            .map { jsonObject -> jsonObject.getAsString("id")!! }
    }.toList().flatten().toSet()

    episodeIds.chunked(25).parallelStream().forEach { episodeIdsChunked ->
        val `object` = runBlocking {
            CrunchyrollWrapper.getObject(
                CountryCode.FR.locale,
                accessToken,
                cms,
                *episodeIdsChunked.toTypedArray()
            )
        }

        `object`.forEach { episodeJson ->
            try {
                episodes.add(
                    crunchyrollPlatform.convertJsonEpisode(
                        CountryCode.FR,
                        episodeJson,
                    )
                )
            } catch (e: Exception) {
                println("Error while converting episode (Episode ID: ${episodeJson.getAsString("id")}): ${e.message}")
                e.printStackTrace()
            }
        }
    }

    httpRequest.close()

    episodes.removeIf { it.releaseDateTime.toLocalDate() !in dates }

    episodes.sortedBy { it.releaseDateTime }.forEach { episode ->
        episode.anime?.releaseDateTime =
            episodes.filter { it.anime?.name == episode.anime?.name }.minOf { it.anime!!.releaseDateTime }
        episodeService.save(episode)
    }

    println("Take ${(System.currentTimeMillis() - start) / 1000}s to check ${dates.size} dates")
}