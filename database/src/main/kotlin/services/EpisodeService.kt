package fr.shikkanime.database.services

import fr.shikkanime.exposed.Transactional
import fr.shikkanime.exposed.TransactionalProxy
import fr.shikkanime.models.Episode
import fr.shikkanime.models.EpisodeLocalization
import fr.shikkanime.models.EpisodePlatform
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
     * Loads every episode of an anime with its images, localizations and platforms.
     *
     * @param animeId identifier of the anime owning the episodes.
     * @return the episodes of the anime with their children, in broadcast order.
     * @see [EpisodeWithChildren]
     */
    @Transactional
    fun findAllByAnimeId(animeId: Uuid): List<EpisodeWithChildren>

    /**
     * Loads an episode with its images, localizations and platforms.
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
     * @param number number of the episode in its season.
     * @param episodeType editorial category of the episode.
     * @return the matching episode aggregate, or `null` when it does not exist.
     * @see [EpisodeWithChildren]
     */
    @Transactional
    fun findByAnimeIdAndSeasonAndNumberAndEpisodeType(
        animeId: Uuid,
        season: Int,
        number: Int,
        episodeType: EpisodeType
    ): EpisodeWithChildren?

    /**
     * Loads the episode released under a platform identifier.
     *
     * @param platform streaming platform owning the release.
     * @param platformId identifier of the episode on the streaming platform.
     * @return the episode released under this identifier, or `null` when none carries it.
     * @see [EpisodeWithChildren]
     */
    @Transactional
    fun findByPlatformAndPlatformId(platform: Platform, platformId: String): EpisodeWithChildren?

    /**
     * Upserts an episode with its images, localizations and platforms.
     *
     * The episode is resolved before any write: a known platform release owns the stored episode,
     * otherwise the identity `(anime, season, type, number)` does. The saved episode owns its
     * children: their owner identifier is written here, exactly once, whatever owner the given
     * [Image], [EpisodeLocalization] and [EpisodePlatform] instances carry. Known localizations and
     * releases are refreshed in place and new ones are appended; nothing is deleted and nothing is
     * recomputed.
     *
     * @param episode aggregate to persist.
     * @return the episode with the images, localizations and platforms persisted by this call.
     * @see [Episode]
     * @see [EpisodeWithChildren]
     */
    @Transactional
    fun save(episode: EpisodeWithChildren): EpisodeWithChildren
}
