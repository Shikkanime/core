package fr.shikkanime.database.repositories

import fr.shikkanime.database.entities.AnimePlatformEntity
import fr.shikkanime.models.AnimePlatform
import kotlin.uuid.Uuid

/**
 * Persistence port of the [AnimePlatform] source links of an anime, keyed by platform identifier.
 *
 * @see [AnimeRepository]
 */
internal abstract class AnimePlatformRepository :
    ModelRepository<Uuid, AnimePlatformEntity, AnimePlatform>(AnimePlatformEntity) {
    /**
     * Loads every platform source link of several animes with one query, grouped by owner.
     *
     * The aggregate composition must not run one lookup per anime: a whole page resolves its links
     * through this single call.
     *
     * @param animeIds identifiers of the animes owning the links.
     * @return the platform links of each anime, an empty list for an anime without link.
     * @see [AnimePlatform]
     */
    abstract fun findAllByAnimeIds(animeIds: List<Uuid>): Map<Uuid?, List<AnimePlatform>>

    /**
     * Upserts one platform source link of an anime, keyed by platform and platform identifier.
     *
     * An already known link is left untouched and a new one is inserted with the supplied
     * identifier. Nothing is deleted.
     *
     * @param animePlatform platform link to persist; its `animeId` must reference the anime.
     * @return the link as stored in database.
     * @see [AnimePlatform]
     */
    abstract fun save(animePlatform: AnimePlatform): AnimePlatform
}
