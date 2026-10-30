package fr.shikkanime.jobs

import fr.shikkanime.core.LoggerFactory
import fr.shikkanime.database.services.AnimeService
import fr.shikkanime.database.services.EpisodeService
import fr.shikkanime.jobs.platforms.PlatformAnime
import fr.shikkanime.jobs.platforms.PlatformEpisode
import fr.shikkanime.models.AnimeWithChildren
import fr.shikkanime.models.Platform
import fr.shikkanime.models.canonicalTitle
import org.koin.core.annotation.Single
import kotlin.uuid.Uuid

/**
 * Saves the animes and episodes harvested from the streaming platforms into the database.
 *
 * An anime is identified across runs by its normalized title: the title shared by most of its
 * localized titles, computed by [canonicalTitle]. One title, one anime - a second run of the same
 * harvest never duplicates a row, and the same anime harvested from two platforms lands on the
 * same row. Its episodes are identified by the anime they belong to, their season, their type and
 * their number, so a release harvested from two platforms lands on the same episode.
 *
 * Every platform link and every platform release of the harvest is persisted. The metadata - the
 * localized texts, the artwork, then the season, the number, the type, the release date and the
 * duration of an episode - belongs to the source with the best [Platform.sortIndex]: a stored row
 * already owned by a better source keeps its metadata while gaining the new links and releases.
 *
 * The save workflow is entered only when a streaming platform reports a change in its payload,
 * through the same change detection [SmartHttpClient] applies to its cached responses: a tick
 * where no platform changed touches nothing, not even a database read.
 */
@Single
internal class HarvestIngestionService(
    private val animeService: AnimeService,
    private val episodeService: EpisodeService
) {
    private val logger = LoggerFactory.getLogger()

    /**
     * Upserts every anime and episode harvested from the streaming platforms.
     *
     * The workflow runs only when at least one platform reports a change in its payload; a quiet
     * harvest is skipped before any database access and nothing is passed to the persistence. When
     * a change is reported, the episodes of every platform are pooled and processed together,
     * changed or not, so the priority rules apply to the whole harvest.
     *
     * @param harvest harvest of every streaming platform, episodes and change flag.
     * @return the number of animes passed to the persistence, `0` when no platform reported a
     * change or the harvest carries no episode.
     * @see [SmartResponse]
     * @see [AnimeService.save]
     * @see [EpisodeService.save]
     */
    fun saveAll(harvest: List<SmartResponse<List<PlatformEpisode>>>): Int {
        // A quiet harvest or one carrying no episode has nothing to persist, so no read either
        if (harvest.none { it.hasChanged } || harvest.all { it.data.isEmpty() }) {
            logger.fine("Nothing to persist, skipping the save workflow")
            return 0
        }

        // Group the pooled harvest by normalized title: one title, one anime
        val episodesByNormalizedTitle = harvest.asSequence()
            .flatMap { it.data.asSequence() }
            .groupBy { it.anime.normalizedTitle }

        // Load in one read the animes already stored under the normalized titles of this harvest
        val storedByNormalizedTitle = animeService
            .findAllByNormalizedTitles(episodesByNormalizedTitle.keys.toList())

        var savedCount = 0

        episodesByNormalizedTitle.forEach { (normalizedTitle, episodes) ->
            val existing = storedByNormalizedTitle[normalizedTitle]
            val animeId = saveAnime(episodes, existing)

            // Every source publishes its episodes; the priority rule protects only their metadata
            episodes.forEach { episode -> saveEpisode(episode, animeId) }

            savedCount++
        }

        return savedCount
    }

    /**
     * Persists the anime of one normalized title and every platform link it is harvested under.
     *
     * The source with the best [Platform.sortIndex] owns the metadata - the localized texts and the
     * artwork. A stored anime already owned by a better source keeps its metadata; the platform
     * links of every source are persisted either way.
     *
     * @param episodes episodes harvested under this normalized title.
     * @param existing stored anime carrying the title, or `null` when it is a new one.
     * @return the identifier of the stored anime.
     * @see [AnimeService.save]
     */
    private fun saveAnime(episodes: List<PlatformEpisode>, existing: AnimeWithChildren?): Uuid {
        val harvests = episodes.map(PlatformEpisode::anime)
            .distinctBy { it.platform to it.id }
        val bestHarvested = harvests.minBy { it.platform.sortIndex }
        val storedSortIndex = existing?.platforms?.minOfOrNull { it.platform.sortIndex }
        val keepsStoredMetadata = storedSortIndex != null &&
            storedSortIndex < bestHarvested.platform.sortIndex

        if (keepsStoredMetadata)
            logger.fine("The anime metadata of '${bestHarvested.normalizedTitle}' is kept, a better source owns it.")

        val model = bestHarvested.toModel()
        val saved = animeService.save(
            model.copy(
                anime = model.anime.copy(id = existing?.anime?.id),
                // An empty child list persists nothing: the stored rows are neither rewritten nor deleted
                images = if (keepsStoredMetadata) emptyList() else model.images,
                localizations = if (keepsStoredMetadata) emptyList() else model.localizations,
                // Every source link is persisted, whatever source owns the metadata
                platforms = harvests.map(PlatformAnime::toPlatformModel)
            )
        )

        return requireNotNull(saved.anime.id)
    }

    /**
     * Persists one harvested episode, keeping the metadata of its best source.
     *
     * The episode is resolved by its platform release, then by its identity under the anime. The
     * source with the best [Platform.sortIndex] owns the metadata - the localized texts, the
     * artwork, the season, the number, the type, the release date and the duration. A stored
     * episode already owned by a better source keeps them; the platform release of this harvest is
     * persisted either way.
     *
     * @param episode harvested episode to persist.
     * @param animeId identifier of the anime owning the episode.
     * @see [EpisodeService.save]
     */
    private fun saveEpisode(episode: PlatformEpisode, animeId: Uuid) {
        val model = episode.toModel()
        val stored = episodeService.findByPlatformAndPlatformId(episode.anime.platform, episode.id)
            // A release stored under another anime is ignored: a renamed anime is a new identity
            ?.takeIf { it.episode.animeId == animeId }
            ?: episodeService.findByAnimeIdAndSeasonAndNumberAndEpisodeType(
                animeId,
                model.episode.season,
                model.episode.number,
                model.episode.episodeType
            )
        val storedSortIndex = stored?.platforms?.minOfOrNull { it.platform.sortIndex }
        val keepsStoredMetadata = storedSortIndex != null &&
            storedSortIndex < episode.anime.platform.sortIndex
        val metadata = if (stored != null && keepsStoredMetadata) stored.episode else model.episode

        episodeService.save(
            model.copy(
                episode = metadata.copy(id = stored?.episode?.id, animeId = animeId),
                // An empty child list persists nothing: the stored rows are neither rewritten nor deleted
                images = if (keepsStoredMetadata) emptyList() else model.images,
                localizations = if (keepsStoredMetadata) emptyList() else model.localizations
            )
        )
    }
}
