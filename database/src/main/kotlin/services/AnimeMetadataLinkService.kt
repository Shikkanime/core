package fr.shikkanime.database.services

import fr.shikkanime.exposed.Transactional
import fr.shikkanime.exposed.TransactionalProxy
import fr.shikkanime.models.AnimeMetadataLink
import fr.shikkanime.models.MetadataPlatform
import kotlin.uuid.Uuid

/**
 * Application service persisting the metadata links of an anime.
 *
 * Each method runs inside a single database transaction and never recomputes editorial data:
 * what is saved is what is given. Consume it through [TransactionalProxy] or the Koin bindings
 * so the [Transactional] methods are actually intercepted.
 *
 * @see [AnimeMetadataLink]
 */
interface AnimeMetadataLinkService {
    /**
     * Loads every metadata link of several animes.
     *
     * @param animeIds identifiers of the animes owning the links.
     * @return the metadata links of each anime, grouped by owner identifier.
     * @see [AnimeMetadataLink]
     */
    @Transactional
    fun findAllByAnimeIds(animeIds: List<Uuid>): Map<Uuid?, List<AnimeMetadataLink>>

    /**
     * Finds one metadata link by its platform and platform identifier.
     *
     * @param platform metadata platform of the link.
     * @param platformId identifier of the entry on the metadata platform.
     * @return the matching link, or `null` when it is not known yet.
     * @see [AnimeMetadataLink]
     */
    @Transactional
    fun findByPlatformAndPlatformId(
        platform: MetadataPlatform,
        platformId: String
    ): AnimeMetadataLink?

    /**
     * Upserts one metadata link of an anime.
     *
     * An already known link is left untouched and a new one is inserted; nothing is deleted.
     *
     * @param link metadata link to persist; its `animeId` must reference the anime.
     * @return the link as stored in database.
     * @see [AnimeMetadataLink]
     */
    @Transactional
    fun save(link: AnimeMetadataLink): AnimeMetadataLink
}
