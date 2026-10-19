package fr.shikkanime.database.services

import fr.shikkanime.exposed.Transactional
import fr.shikkanime.exposed.TransactionalProxy
import fr.shikkanime.models.Anime
import fr.shikkanime.models.AnimeLocalization
import fr.shikkanime.models.AnimePlatform
import fr.shikkanime.models.AnimeWithChildren
import fr.shikkanime.models.Image
import fr.shikkanime.models.Platform
import fr.shikkanime.models.canonicalTitle
import kotlin.uuid.Uuid

/**
 * Application service persisting the anime aggregate.
 *
 * Each method runs inside a single database transaction and never recomputes editorial data:
 * what is saved is what is given. Consume it through [TransactionalProxy] or the Koin bindings so
 * the [Transactional] methods are actually intercepted.
 */
interface AnimeService {
    /**
     * Loads every anime of the database with its images, localizations and platforms.
     *
     * @return every stored anime aggregate, in no particular order.
     * @see [AnimeWithChildren]
     */
    @Transactional
    fun findAll(): List<AnimeWithChildren>

    /**
     * Loads the anime aggregates carrying these normalized titles.
     *
     * The column is unique: one anime per normalized title. One grouped read answers them all,
     * and a title carried by no anime is simply absent from the result.
     *
     * @param titles normalized titles to look up, computed by [canonicalTitle].
     * @return the matching animes with their children, indexed by normalized title.
     * @see [AnimeWithChildren]
     */
    @Transactional
    fun findAllByNormalizedTitles(titles: List<String>): Map<String, AnimeWithChildren>

    /**
     * Loads the anime aggregate carrying this normalized title.
     *
     * @param normalizedTitle normalized title, computed by [canonicalTitle].
     * @return the single anime carrying this title with its children, or null.
     * @see [AnimeWithChildren]
     */
    @Transactional
    fun findByNormalizedTitle(normalizedTitle: String): AnimeWithChildren?

    /**
     * Loads the anime aggregate linked to a streaming platform identifier.
     *
     * @param platform streaming platform owning the link.
     * @param platformId identifier of the anime on the streaming platform.
     * @return the linked anime with its children, or `null` when no anime carries this link.
     * @see [AnimeWithChildren]
     */
    @Transactional
    fun findByPlatformAndPlatformId(platform: Platform, platformId: String): AnimeWithChildren?

    /**
     * Loads an anime with its images, localizations and platforms.
     *
     * @param id identifier of the anime to load.
     * @return the anime aggregate, or `null` when no anime has this identifier.
     * @see [AnimeWithChildren]
     */
    @Transactional
    fun findById(id: Uuid): AnimeWithChildren?

    /**
     * Upserts an anime with its images, localizations and platforms.
     *
     * The saved anime owns its children: their owner identifier is written here, exactly once,
     * whatever owner the given [Image], [AnimeLocalization] and [AnimePlatform] instances carry.
     * Known localizations and images are refreshed in place and new ones are appended; nothing is
     * deleted and nothing is recomputed. The returned aggregate is composed from the rows this
     * call just persisted.
     *
     * @param anime aggregate to persist.
     * @return the anime with the images, localizations and platforms persisted by this call.
     * @see [Anime]
     * @see [AnimeWithChildren]
     */
    @Transactional
    fun save(anime: AnimeWithChildren): AnimeWithChildren
}
