package fr.shikkanime.database.services

import fr.shikkanime.exposed.Transactional
import fr.shikkanime.models.Anime
import fr.shikkanime.models.Platform
import kotlin.uuid.Uuid

/**
 * Application service persisting the anime aggregate.
 *
 * Each method runs inside a single database transaction and never recomputes editorial data:
 * what is saved is what is given. Consume it through
 * [fr.shikkanime.exposed.TransactionalProxy] or the Koin bindings so the [Transactional] methods
 * are actually intercepted.
 */
interface AnimeService {
    /**
     * Loads every anime of the database with its images, localizations and platforms.
     *
     * @return every stored anime, in no particular order.
     */
    @Transactional
    fun findAll(): List<Anime>

    /**
     * Loads the anime linked to a streaming platform identifier.
     *
     * @param platform streaming platform owning the link.
     * @param platformId identifier of the anime on the streaming platform.
     * @return the linked anime, or `null` when no anime carries this platform link.
     */
    @Transactional
    fun findByPlatformAndPlatformId(platform: Platform, platformId: String): Anime?

    /**
     * Loads an anime with its images, localizations and platforms.
     *
     * @param id identifier of the anime to load.
     * @return the anime aggregate, or `null` when no anime has this identifier.
     */
    @Transactional
    fun findById(id: Uuid): Anime?

    /**
     * Upserts an anime with its images, localizations and platforms.
     *
     * Images are always stored under [Anime.id] as owner, whatever owner the given
     * [fr.shikkanime.models.Image] instances carry. Known localizations and images are refreshed
     * in place and new ones are appended; nothing is deleted and nothing is recomputed.
     *
     * @param anime aggregate to persist.
     * @return the anime with the images, localizations and platforms persisted by this call.
     */
    @Transactional
    fun save(anime: Anime): Anime
}
