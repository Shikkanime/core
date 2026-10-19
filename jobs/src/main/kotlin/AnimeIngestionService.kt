package fr.shikkanime.jobs

import fr.shikkanime.core.LoggerFactory
import fr.shikkanime.database.services.AnimeService
import fr.shikkanime.jobs.platforms.PlatformAnime
import fr.shikkanime.models.Platform
import fr.shikkanime.models.canonicalTitle
import org.koin.core.annotation.Single

/**
 * Saves the animes harvested from a streaming platform into the database.
 *
 * An anime is identified across runs by its normalized title: the title shared by most of its
 * localized titles, computed by [canonicalTitle]. One title, one anime - a second run of the same
 * harvest never duplicates a row, and the same anime harvested from two platforms lands on the
 * same row.
 *
 * The harvests of one run are pooled and grouped by normalized title: the source with the best
 * [Platform.sortIndex] owns the anime, and a harvest from a lower priority source than the stored
 * one is skipped with a warning instead of overwriting its data.
 */
@Single
internal class AnimeIngestionService(
    private val animeService: AnimeService
) {
    private val logger = LoggerFactory.getLogger()

    /**
     * Upserts every anime harvested from a platform.
     *
     * @param animes normalized animes to persist, without duplicates.
     * @return the number of animes passed to the persistence.
     * @see [AnimeService.save]
     */
    fun saveAll(animes: List<PlatformAnime>): Int {
        val harvestsByNormalizedTitle = animes.groupBy { it.normalizedTitle }

        // Load in one read the animes already stored under the normalized titles of this harvest
        val storedByNormalizedTitle = animeService
            .findAllByNormalizedTitles(harvestsByNormalizedTitle.keys.toList())

        var savedCount = 0

        harvestsByNormalizedTitle.forEach { (normalizedTitle, harvests) ->
            val existing = storedByNormalizedTitle[normalizedTitle]
            val candidate = harvests.minBy { it.platform.sortIndex }

            val storedSortIndex = existing?.platforms?.minOf { it.platform.sortIndex }
            val candidateSortIndex = candidate.platform.sortIndex

            if (storedSortIndex != null
                && storedSortIndex < candidateSortIndex
            ) {
                logger.warning(
                    "A minus priority platform for '$normalizedTitle' is being ingested, " +
                        "skipping it."
                )
                return@forEach
            }

            val model = candidate.toModel()

            // Persist the harvest; the stored identity is re-applied so no row is duplicated
            animeService.save(model.copy(anime = model.anime.copy(id = existing?.anime?.id)))
            savedCount++
        }

        return savedCount
    }
}
