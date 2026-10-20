package fr.shikkanime.jobs

import fr.shikkanime.core.LoggerFactory
import fr.shikkanime.database.services.AnimeService
import fr.shikkanime.jobs.platforms.PlatformAnime
import fr.shikkanime.jobs.platforms.PlatformEpisode
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
 *
 * The save workflow is entered only when the harvested content changed since the last successful
 * save: the content keys of the harvest are kept in memory and compared before any database
 * access. The single writer of these keys is the scheduled job (annotated
 * `@DisallowConcurrentExecution`) or the one-shot entrypoint, so the field needs no guard.
 */
@Single
internal class AnimeIngestionService(
    private val animeService: AnimeService
) {
    private val logger = LoggerFactory.getLogger()

    private var lastSavedContentKeys: Set<List<Any?>> = emptySet()

    /**
     * Derives the content key of a harvested anime.
     *
     * The key lists every constructor field of the anime as plain values, artwork and localizations
     * included, so two animes carrying the same content share the same key whatever their instance.
     *
     * @receiver the anime carried by a harvested episode.
     * @return the content key of the anime.
     * @see [PlatformAnime]
     */
    private fun PlatformAnime.contentKey(): List<Any?> =
        listOf(
            id,
            platform,
            images.map { listOf(it.type, it.url) },
            localizations.map { listOf(it.country, it.locale, it.title, it.description) }
        )

    /**
     * Derives one content key per distinct episode of a harvest.
     *
     * A key lists every constructor field of the harvested graph as plain values, the carried anime
     * contributing its own [contentKey]: two harvests carrying the same content share the same keys
     * whatever their order or their instances. Only plain fields are read, so no conversion runs on
     * an unchanged run. A field added to [PlatformEpisode], [PlatformAnime] or their children must
     * be added here too, or its changes go undetected.
     *
     * @receiver the episodes harvested from the streaming platforms.
     * @return the content keys of the harvest, one per distinct episode.
     * @see [contentKey]
     */
    private fun List<PlatformEpisode>.contentKeys(): Set<List<Any?>> {
        // The episodes of a show share their anime: its key is derived once per distinct instance
        val animeKeys = mutableMapOf<PlatformAnime, List<Any?>>()

        return map { episode ->
            listOf(
                animeKeys.getOrPut(episode.anime) { episode.anime.contentKey() },
                episode.id,
                episode.image,
                episode.releaseDateTime,
                episode.season,
                episode.number,
                episode.episodeType,
                episode.duration,
                episode.url,
                episode.audioLocales,
                episode.uncensored,
                episode.localizations.map { listOf(it.country, it.title, it.description) }
            )
        }
            .toSet()
    }

    /**
     * Upserts every anime harvested from the streaming platforms.
     *
     * The workflow runs only for a harvest whose content changed since the last successful save:
     * an empty or unchanged harvest is skipped before any database access and nothing is passed to
     * the persistence. One entry per harvested episode is expected - duplicates collapse in the
     * content keys - so the caller maps one entry per episode.
     *
     * @param episodes episodes harvested from the streaming platforms; the animes they carry are
     * the data being persisted.
     * @return the number of animes passed to the persistence, `0` when the harvest is empty or
     * unchanged since the last successful save.
     * @see [contentKeys]
     * @see [AnimeService.save]
     */
    fun saveAll(episodes: List<PlatformEpisode>): Int {
        val contentKeys = episodes.contentKeys()

        if (contentKeys.isEmpty()
            || contentKeys == lastSavedContentKeys
        ) {
            logger.fine("Harvest empty or unchanged since the last save, skipping the save workflow")
            return 0
        }

        val animes = episodes.map(PlatformEpisode::anime)
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

        // The keys are remembered only once the workflow completed, so a failed run is retried
        lastSavedContentKeys = contentKeys
        return savedCount
    }
}
