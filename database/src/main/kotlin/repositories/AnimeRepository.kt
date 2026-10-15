package fr.shikkanime.database.repositories

import fr.shikkanime.database.entities.AnimeEntity
import fr.shikkanime.models.Anime
import fr.shikkanime.models.Platform
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
     * Loads every anime of the database with its children.
     *
     * @return every stored anime, in no particular order.
     */
    abstract fun findAll(): List<Anime>

    /**
     * Loads the anime linked to a streaming platform identifier.
     *
     * @param platform streaming platform owning the link.
     * @param platformId identifier of the anime on the streaming platform.
     * @return the linked anime, or `null` when no anime carries this platform link.
     */
    abstract fun findByPlatformAndPlatformId(platform: Platform, platformId: String): Anime?

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
