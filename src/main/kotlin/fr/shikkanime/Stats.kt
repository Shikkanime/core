package fr.shikkanime

import fr.shikkanime.services.AnimeService
import fr.shikkanime.services.EpisodeMappingService
import fr.shikkanime.services.EpisodeVariantService
import fr.shikkanime.services.SimulcastService
import fr.shikkanime.utils.Constant
import kotlin.system.exitProcess

fun main() {
    val simulcastService = Constant.injector.getInstance(SimulcastService::class.java)
    val animeService = Constant.injector.getInstance(AnimeService::class.java)
    val episodeMappingService = Constant.injector.getInstance(EpisodeMappingService::class.java)
    val episodeVariantService = Constant.injector.getInstance(EpisodeVariantService::class.java)

    val simulcasts = simulcastService.findAll()
    val animes = animeService.findAll()

    val previousAnimes = animes.filter { it.simulcasts.any { simulcast -> simulcast.uuid == simulcasts[1].uuid } }
    val previousPlatformCount = previousAnimes.flatMap { episodeVariantService.findAllByAnime(it) }
        .groupBy { it.platform!! }
        .mapValues { (_, value) -> value.map { it.mapping!!.anime!! }.distinctBy { it.uuid }.size }
    val previousDubbedAnimes = previousAnimes.filter { episodeVariantService.findAllAudioLocalesByAnime(it).contains("fr-FR") }.size
    val previousMostReleaseDay = previousAnimes.flatMap { episodeMappingService.findAllByAnime(it) }
        .groupBy { it.releaseDateTime.dayOfWeek }
        .mapValues { (_, value) -> value.map { it.anime!! }.distinctBy { it.uuid }.size }

    val actualAnimes = animes.filter { it.simulcasts.any { simulcast -> simulcast.uuid == simulcasts.first().uuid } }
    val actualPlatformCount = actualAnimes.flatMap { episodeVariantService.findAllByAnime(it) }
        .groupBy { it.platform!! }
        .mapValues { (_, value) -> value.map { it.mapping!!.anime!! }.distinctBy { it.uuid }.size }
    val actualDubbedAnimes = actualAnimes.filter { episodeVariantService.findAllAudioLocalesByAnime(it).contains("fr-FR") }.size
    val actualMostReleaseDay = actualAnimes.flatMap { episodeMappingService.findAllByAnime(it) }
        .groupBy { it.releaseDateTime.dayOfWeek }
        .mapValues { (_, value) -> value.map { it.anime!! }.distinctBy { it.uuid }.size }

    println("Previous simulcast animes size: ${previousAnimes.size}")
    println("Actual simulcast animes size: ${actualAnimes.size} (${stringSign(actualAnimes.size - previousAnimes.size)})")

    println("\nPrevious simulcast platform animes size:")
    previousPlatformCount.toSortedMap()
        .forEach { (platform, count) -> println("  ${platform.platformName}: $count") }

    println("Actual simulcast platform animes size:")
    actualPlatformCount.toSortedMap()
        .forEach { (platform, count) -> println("  ${platform.platformName}: $count (${stringSign(count - (previousPlatformCount[platform] ?: 0))})") }

    println("\nDubbed previous simulcast animes size: $previousDubbedAnimes")
    println("Dubbed actual simulcast animes size: $actualDubbedAnimes (${stringSign(actualDubbedAnimes - previousDubbedAnimes)})")

    println("\nPrevious simulast most release day:")
    previousMostReleaseDay.toSortedMap()
        .forEach { (day, count) -> println("  $day: $count") }

    println("Actual simulcast most release day:")
    actualMostReleaseDay.toSortedMap()
        .forEach { (day, count) -> println("  $day: $count (${stringSign(count - (previousMostReleaseDay[day] ?: 0))})") }

    exitProcess(0)
}

private fun stringSign(value: Int) = (if (value > 0) "+" else "") + value