package fr.shikkanime

import fr.shikkanime.entities.enums.CountryCode
import fr.shikkanime.services.AnimeService
import fr.shikkanime.services.EpisodeVariantService
import fr.shikkanime.utils.Constant
import fr.shikkanime.wrappers.factories.AbstractAniListWrapper
import fr.shikkanime.wrappers.impl.caches.AniListCachedWrapper
import java.time.ZonedDateTime
import kotlin.system.exitProcess

suspend fun main() {
    val animeService = Constant.injector.getInstance(AnimeService::class.java)
    val episodeVariantService = Constant.injector.getInstance(EpisodeVariantService::class.java)

    val randomAnime = animeService.findAll().random()
    println("Anime: ${randomAnime.name}, slug: ${randomAnime.slug}")

    val episodes = episodeVariantService.findAllByAnime(randomAnime.uuid!!)
        .filterNot { it.audioLocale == CountryCode.FR.locale }
    val latestRelease = episodes.maxOfOrNull { it.releaseDateTime } ?: run {
        println("No episodes found")
        exitProcess(1)
    }

    val isReleasing = latestRelease.isAfter(ZonedDateTime.now().minusWeeks(2))
    println("Is releasing: $isReleasing (latest episode release: $latestRelease)")

    val medias = AniListCachedWrapper.search(
        query = randomAnime.name!!,
        status = if (isReleasing) listOf(AbstractAniListWrapper.Status.RELEASING, AbstractAniListWrapper.Status.FINISHED)
        else listOf(AbstractAniListWrapper.Status.FINISHED)
    ).filter { it.format != "MUSIC" && it.externalLinks?.any { link -> link.type == "STREAMING" } == true }

    println("Found ${medias.size} medias on AniList")
    medias.forEach(::println)

    val filteredMedias = medias.sortedBy { it.id }.filter { media ->
        val hasPrequel = media.relations?.edges?.any { it.relationType == "PREQUEL" } ?: false
        val hasSideStory = media.relations?.edges?.any { it.relationType == "SIDE_STORY" } ?: false
        !hasPrequel || hasSideStory
    }.distinctBy { it.id }

    println("Found ${filteredMedias.size} filtered medias on AniList")
    filteredMedias.forEach(::println)

    if (filteredMedias.size > 1) {
        val exactMatches = filteredMedias.filter {
            it.title.romaji.equals(randomAnime.name, ignoreCase = true) ||
                    it.title.english.equals(randomAnime.name, ignoreCase = true) ||
                    it.title.native.equals(randomAnime.name, ignoreCase = true)
        }
        if (exactMatches.size == 1) println("Found exact match: ${exactMatches.first()}")
    }

    exitProcess(0)
}
