package fr.shikkanime.database.repositories

import fr.shikkanime.database.entities.EpisodeEntity
import fr.shikkanime.exposed.repositories.EntityRepository
import fr.shikkanime.models.Episode
import fr.shikkanime.models.EpisodeType
import fr.shikkanime.models.EpisodeWithChildren
import fr.shikkanime.models.Platform
import kotlin.uuid.Uuid

/**
 * Persistence port of the episode aggregate, read as [EpisodeWithChildren].
 *
 * Only the identity row is written here and [save] returns that flat [Episode] row: children are
 * persisted by [EpisodeLocalizationRepository], [EpisodeReleaseRepository] and [ImageRepository],
 * and the caller composes the aggregate from those fresh rows. Every read is composed with one
 * batch read per relation, never with a lazy walk.
 *
 * @see [EpisodeWithChildren]
 */
internal abstract class EpisodeRepository : EntityRepository<Uuid, EpisodeEntity>(EpisodeEntity) {
    /**
     * Loads every episode of an anime with its children.
     *
     * The episodes are ordered by season then number, the broadcast order of an anime.
     *
     * @param animeId identifier of the anime owning the episodes.
     * @return the episodes of the anime with their children, in broadcast order.
     * @see [EpisodeWithChildren]
     */
    abstract fun findAllByAnimeId(animeId: Uuid): List<EpisodeWithChildren>

    /**
     * Loads one episode aggregate by its identifier.
     *
     * @param id identifier of the episode to load.
     * @return the matching episode with its children, or `null` when it does not exist.
     * @see [EpisodeWithChildren]
     */
    abstract fun findModelById(id: Uuid): EpisodeWithChildren?

    /**
     * Loads the episode aggregate carrying this identity.
     *
     * An episode is identified by its anime, its season, its type and its number: a release
     * harvested from two platforms resolves to the same row.
     *
     * @param animeId identifier of the anime owning the episode.
     * @param season season of the episode.
     * @param episodeType editorial category of the episode.
     * @param number number of the episode in its season.
     * @return the matching episode with its children, or `null` when it does not exist.
     * @see [EpisodeWithChildren]
     */
    abstract fun findByAnimeIdSeasonEpisodeTypeAndNumber(
        animeId: Uuid,
        season: Int,
        episodeType: EpisodeType,
        number: Int
    ): EpisodeWithChildren?

    /**
     * Loads the episode aggregate carrying a release.
     *
     * A video published in several audio locales carries several releases, all owned by the same
     * episode: the first one answers.
     *
     * @param platform streaming platform owning the release.
     * @param platformId identifier of the video on the streaming platform.
     * @return the episode released under this platform identifier, or `null` when none carries it.
     * @see [EpisodeWithChildren]
     */
    abstract fun findByPlatformAndPlatformId(platform: Platform, platformId: String): EpisodeWithChildren?

    /**
     * Persists the episode identity row and returns that row.
     *
     * A known episode takes the metadata of the given row - season, type, number, release date and
     * duration - because the caller decides which source owns them. The children are upserted right
     * after by the caller, which composes the aggregate from those fresh rows.
     *
     * @param episode episode row to persist; its children are not written here.
     * @return the stored episode row.
     * @see [Episode]
     */
    abstract fun save(episode: Episode): Episode
}
