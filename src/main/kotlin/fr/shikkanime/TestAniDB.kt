package fr.shikkanime

import fr.shikkanime.entities.enums.CountryCode
import fr.shikkanime.services.AnimePlatformService
import fr.shikkanime.services.AnimeService
import fr.shikkanime.services.EpisodeVariantService
import fr.shikkanime.utils.Constant
import fr.shikkanime.wrappers.impl.caches.AniListCachedWrapper
import java.io.File
import kotlin.system.exitProcess

suspend fun main() {
    val tmpDbbFile = File("shikkanime-test.db")
    if (!tmpDbbFile.exists())
        tmpDbbFile.createNewFile()
    val animeService = Constant.injector.getInstance(AnimeService::class.java)
    val animePlatformService = Constant.injector.getInstance(AnimePlatformService::class.java)
    val episodeVariantService = Constant.injector.getInstance(EpisodeVariantService::class.java)
    val animes = animeService.findAll()

    while (true) {
        val lines = tmpDbbFile.readLines()
        val randomAnime = animes.filterNot { lines.any { line -> line.startsWith(it.uuid!!.toString()) } }.random()
        val episodes = episodeVariantService.findAllByAnime(randomAnime.uuid!!).filterNot { it.audioLocale == CountryCode.FR.locale }

        val firstReleaseDateTime = episodes.minOfOrNull { it.releaseDateTime } ?: run {
            println("No episodes found for the anime: ${randomAnime.name!!}")
            continue
        }

        val latestReleaseDateTime = episodes.maxOfOrNull { it.releaseDateTime } ?: run {
            println("No episodes found for the anime: ${randomAnime.name!!}")
            continue
        }

        val platforms = animePlatformService.findAllByAnime(randomAnime.uuid)
        val media = AniListCachedWrapper.findAnilistMedia(randomAnime.name!!, platforms, firstReleaseDateTime.year, latestReleaseDateTime)

        if (media != null) {
            println("\n\nFound AniList media: $media")
            tmpDbbFile.appendText("${randomAnime.uuid},${media.id}\n")
        } else {
            println("\n\nNo AniList media found for anime: ${randomAnime.name}")
        }

        println("-".repeat(80))
        println("Type 'continue' to continue or 'exit' to exit")
        val input = readlnOrNull()

        if (input.equals("exit", ignoreCase = true)) {
            break
        } else if (!input.equals("continue", ignoreCase = true)) {
            println("Invalid input, exiting...")
            break
        }
    }

    exitProcess(0)
}
