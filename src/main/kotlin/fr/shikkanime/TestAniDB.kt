package fr.shikkanime

import fr.shikkanime.entities.enums.LangType
import fr.shikkanime.services.AnimePlatformService
import fr.shikkanime.services.caches.AnimeCacheService
import fr.shikkanime.utils.Constant
import fr.shikkanime.wrappers.impl.caches.AniListCachedWrapper
import java.io.File
import kotlin.system.exitProcess

suspend fun main() {
    val tmpDbbFile = File("shikkanime-test.db")
    if (!tmpDbbFile.exists())
        tmpDbbFile.createNewFile()
    val animeCacheService = Constant.injector.getInstance(AnimeCacheService::class.java)
    val animePlatformService = Constant.injector.getInstance(AnimePlatformService::class.java)
    val animes = animeCacheService.findAll()

    while (true) {
        val lines = tmpDbbFile.readLines()
        val randomAnime = animes.filterNot {
            val langTypes = animeCacheService.getLangTypes(it)
            (langTypes.size == 1 && langTypes[0] == LangType.VOICE) || lines.any { line -> line.startsWith(it.uuid!!.toString()) }
        }.random()
        val platforms = animePlatformService.findAllByAnime(randomAnime.uuid!!)
        val media = AniListCachedWrapper.findAnilistMedia(randomAnime.name!!, platforms, randomAnime.releaseDateTime.year)

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
