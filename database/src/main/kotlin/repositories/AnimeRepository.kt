package fr.shikkanime.database.repositories

import fr.shikkanime.database.entities.AnimeEntity
import fr.shikkanime.models.Anime
import kotlin.uuid.Uuid

/**
 * Persistence port of the [Anime] aggregate root.
 *
 * Only the identity row is written here: children are persisted by
 * [AnimeLocalizationRepository], [AnimePlatformRepository] and [ImageRepository], and read back
 * through the lazy relations of [AnimeEntity].
 */
internal abstract class AnimeRepository :
    ModelRepository<Uuid, AnimeEntity, Anime>(AnimeEntity) {
    /**
     * Creates the anime identity row when it does not exist yet.
     *
     * The anime carries no mutable column, so an already known anime is returned untouched.
     *
     * @param anime aggregate root to persist.
     * @return the anime as stored in database.
     */
    abstract fun save(anime: Anime): Anime
}
