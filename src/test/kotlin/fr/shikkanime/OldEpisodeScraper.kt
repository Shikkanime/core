package fr.shikkanime

import fr.shikkanime.entities.enums.CountryCode
import fr.shikkanime.platforms.AnimationDigitalNetworkPlatform
import fr.shikkanime.services.EpisodeService
import fr.shikkanime.utils.Constant
import fr.shikkanime.utils.HttpRequest
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

    println("Checking ${dates.size} dates...")

    val episodeService = Constant.injector.getInstance(EpisodeService::class.java)
    val adnPlatform = Constant.injector.getInstance(AnimationDigitalNetworkPlatform::class.java)

    dates.forEach { date ->
        runBlocking { adnPlatform.fetchApiContent(CountryCode.FR, date) }.forEach { episodeJson ->
            try {
                adnPlatform.convertEpisode(
                    CountryCode.FR,
                    episodeJson.asJsonObject,
                    date
                ).forEach {
                    episodeService.save(it)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    httpRequest.close()

    println("Take ${(System.currentTimeMillis() - start) / 1000}s to check ${dates.size} dates")
}