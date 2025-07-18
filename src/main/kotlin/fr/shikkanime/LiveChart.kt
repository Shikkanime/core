package fr.shikkanime

import fr.shikkanime.utils.atStartOfWeek
import fr.shikkanime.wrappers.impl.caches.LiveChartCachedWrapper
import java.time.LocalDate

suspend fun main() {
    val animeIds = (0L..1L).flatMap { LiveChartCachedWrapper.getAnimeIdsFromDate(LocalDate.now().minusWeeks(it).atStartOfWeek()) }.toSet()
    println("Found ${animeIds.size} anime with streams available.")
    val ids = animeIds.flatMap { LiveChartCachedWrapper.getStreamsForAnime(it) }.toSet()
    // Affichage des IDs trouvées
    ids.sortedBy { it.first.platformName }.forEach { (platform, id) ->
        println("${platform.name}: $id")
    }
}