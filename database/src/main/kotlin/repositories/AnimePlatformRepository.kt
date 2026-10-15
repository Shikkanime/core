package fr.shikkanime.database.repositories

import fr.shikkanime.database.entities.AnimePlatformEntity
import fr.shikkanime.models.Anime
import fr.shikkanime.models.AnimePlatform
import kotlin.uuid.Uuid

/**
 * Persistence port of the [AnimePlatform] source links of an anime, keyed by platform identifier.
 *
 * @see AnimeRepository
 */
internal abstract class AnimePlatformRepository :
    ModelRepository<Uuid, AnimePlatformEntity, AnimePlatform>(AnimePlatformEntity) {
    /**
     * Upserts one platform source link of an anime, keyed by platform and platform identifier.
     *
     * An already known link is left untouched and a new one is inserted with the supplied
     * identifier. Nothing is deleted.
     *
     * @param anime anime owning the link, already loaded by the caller.
     * @param animePlatform platform link to persist.
     * @return the link as stored in database.
     */
    abstract fun save(anime: Anime, animePlatform: AnimePlatform): AnimePlatform
}
