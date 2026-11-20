package fr.shikkanime.database.repositories

import fr.shikkanime.database.entities.AnimeMetadataLinkEntity
import fr.shikkanime.models.AnimeMetadataLink
import fr.shikkanime.models.MetadataPlatform
import kotlin.uuid.Uuid

/**
 * Persistence port of the [AnimeMetadataLink] source links of an anime, keyed by platform
 * identifier.
 *
 * @see [AnimeRepository]
 */
internal abstract class AnimeMetadataLinkRepository :
    ModelRepository<Uuid, AnimeMetadataLinkEntity, AnimeMetadataLink>(AnimeMetadataLinkEntity) {
    /**
     * Loads every metadata link of the database.
     *
     * @return every stored link, in no particular order.
     * @see [AnimeMetadataLink]
     */
    abstract fun findAllModels(): List<AnimeMetadataLink>

    /**
     * Loads every metadata link of several animes with one query, grouped by owner.
     *
     * @param animeIds identifiers of the animes owning the links.
     * @return the metadata links of each anime, an empty list for an anime without link.
     * @see [AnimeMetadataLink]
     */
    abstract fun findAllByAnimeIds(animeIds: List<Uuid>): Map<Uuid?, List<AnimeMetadataLink>>

    /**
     * Finds one metadata link by its platform and platform identifier.
     *
     * @param platform metadata platform of the link.
     * @param platformId identifier of the entry on the metadata platform.
     * @return the matching link, or `null` when it is not known yet.
     * @see [AnimeMetadataLink]
     */
    abstract fun findByPlatformAndPlatformId(
        platform: MetadataPlatform,
        platformId: String
    ): AnimeMetadataLink?

    /**
     * Upserts one metadata link of an anime, keyed by anime, platform and platform identifier.
     *
     * An already known link is left untouched and a new one is inserted with the supplied
     * identifier. Nothing is deleted.
     *
     * @param link metadata link to persist; its `animeId` must reference the anime.
     * @return the link as stored in database.
     * @see [AnimeMetadataLink]
     */
    abstract fun save(link: AnimeMetadataLink): AnimeMetadataLink
}
