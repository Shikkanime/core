package fr.shikkanime.database.services

import fr.shikkanime.core.pagination.Page
import fr.shikkanime.core.pagination.PageRequest
import fr.shikkanime.exposed.Transactional
import fr.shikkanime.exposed.TransactionalProxy
import fr.shikkanime.models.Episode
import fr.shikkanime.models.EpisodeLocalization
import fr.shikkanime.models.EpisodeRelease
import fr.shikkanime.models.EpisodeType
import fr.shikkanime.models.EpisodeWithChildren
import fr.shikkanime.models.Image
import fr.shikkanime.models.Platform
import kotlin.uuid.Uuid

/**
 * Application service persisting the episode aggregate.
 *
 * Each method runs inside a single database transaction and never recomputes editorial data:
 * what is saved is what is given. Consume it through [TransactionalProxy] or the Koin bindings so
 * the [Transactional] methods are actually intercepted.
 *
 * @see [EpisodeWithChildren]
 */
interface EpisodeService {
    /**
     * Loads every episode of an anime with its images, localizations and releases.
     *
     * @param animeId identifier of the anime owning the episodes.
     * @return the episodes of the anime with their children, in broadcast order.
     * @see [EpisodeWithChildren]
     */
    @Transactional
    fun findAllByAnimeId(animeId: Uuid): List<EpisodeWithChildren>

    /**
     * Loads one page of the episodes of an anime with their images, localizations and releases.
     *
     * @param animeId identifier of the anime owning the episodes.
     * @param request page and size to read.
     * @return the requested page of episode aggregates, in broadcast order.
     * @see [EpisodeWithChildren]
     */
    @Transactional
    fun findPageByAnimeId(animeId: Uuid, request: PageRequest): Page<EpisodeWithChildren>

    /**
     * Loads one page of every episode, newest release first.
     *
     * The order follows the broadcast date of the episode, not the date of its platform releases.
     *
     * @param request page and size to read.
     * @return the requested page of episode aggregates, newest release first.
     * @see [EpisodeWithChildren]
     */
    @Transactional
    fun findPageByReleaseDate(request: PageRequest): Page<EpisodeWithChildren>

    /**
     * Loads an episode with its images, localizations and releases.
     *
     * @param id identifier of the episode to load.
     * @return the episode aggregate, or `null` when no episode has this identifier.
     * @see [EpisodeWithChildren]
     */
    @Transactional
    fun findById(id: Uuid): EpisodeWithChildren?

    /**
     * Loads the episode carrying an identity under an anime.
     *
     * @param animeId identifier of the anime owning the episode.
     * @param season season of the episode.
     * @param episodeType editorial category of the episode.
     * @param number number of the episode in its season.
     * @return the matching episode aggregate, or `null` when it does not exist.
     * @see [EpisodeWithChildren]
     */
    @Transactional
    fun findByAnimeIdSeasonEpisodeTypeAndNumber(
        animeId: Uuid,
        season: Int,
        episodeType: EpisodeType,
        number: Int
    ): EpisodeWithChildren?

    /**
     * Loads the episode released under a platform identifier.
     *
     * A video published in several audio locales carries several releases, all owned by the same
     * episode: the first one answers.
     *
     * @param platform streaming platform owning the release.
     * @param platformId identifier of the video on the streaming platform.
     * @return the episode released under this identifier, or `null` when none carries it.
     * @see [EpisodeWithChildren]
     */
    @Transactional
    fun findByPlatformAndPlatformId(platform: Platform, platformId: String): EpisodeWithChildren?

    /**
     * Upserts an episode with its images, localizations and releases.
     *
     * The identity is resolved by the caller, never recomputed here: the aggregate carries the
     * stored episode identifier when it updates a known row, and no identifier when it inserts a new
     * one. Resolving it twice would replay the same lookups, and Exposed has no read cache to absorb
     * them. The saved episode owns its children: their owner identifier is written here, exactly
     * once, whatever owner the given [Image], [EpisodeLocalization] and [EpisodeRelease] instances
     * carry. Known localizations and releases are refreshed in place and new ones are appended;
     * nothing is deleted and nothing is recomputed.
     *
     * @param episode aggregate to persist.
     * @return the episode with the images, localizations and releases persisted by this call.
     * @see [Episode]
     * @see [EpisodeWithChildren]
     */
    @Transactional
    fun save(episode: EpisodeWithChildren): EpisodeWithChildren
}
