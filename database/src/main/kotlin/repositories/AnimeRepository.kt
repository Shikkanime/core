package fr.shikkanime.database.repositories

import fr.shikkanime.database.entities.AnimeEntity
import fr.shikkanime.exposed.repositories.EntityRepository
import fr.shikkanime.models.Anime
import fr.shikkanime.models.AnimeWithChildren
import fr.shikkanime.models.Platform
import kotlin.uuid.Uuid

/**
 * Persistence port of the anime aggregate, read as [AnimeWithChildren].
 *
 * Only the identity row is written here and [save] returns that flat [Anime] row: children are
 * persisted by [AnimeLocalizationRepository], [AnimePlatformRepository] and [ImageRepository], and
 * the caller composes the aggregate from those fresh rows. Every read is composed with one batch
 * read per relation, never with a lazy walk.
 */
internal abstract class AnimeRepository : EntityRepository<Uuid, AnimeEntity>(AnimeEntity) {
    /**
     * Loads every anime of the database with its children.
     *
     * The entity-level read comes from the framework's `EntityRepository.findAll()`, called through
     * `super.findAll()`: the repository layer owns every query.
     *
     * @return every stored anime aggregate, in no particular order.
     * @see [AnimeWithChildren]
     */
    abstract fun findAllModels(): List<AnimeWithChildren>

    /**
     * Loads the anime aggregates carrying these normalized titles.
     *
     * The column is unique: one anime per normalized title. Every row is read in one query and
     * composed with the same one-batch-read-per-relation strategy as the other finders; a title
     * carried by no anime is simply absent from the result.
     *
     * @param titles normalized titles to look up.
     * @return the matching animes with their children, indexed by normalized title.
     * @see [AnimeWithChildren]
     */
    abstract fun findAllByNormalizedTitles(titles: List<String>): Map<String, AnimeWithChildren>

    /**
     * Loads one anime aggregate by its identifier.
     *
     * @param id identifier of the anime to load.
     * @return the matching anime with its children, or `null` when it does not exist.
     * @see [AnimeWithChildren]
     */
    abstract fun findModelById(id: Uuid): AnimeWithChildren?

    /**
     * Loads the anime aggregate carrying this normalized title.
     *
     * The column is unique: one anime per normalized title. A title really shared by two animes
     * (a film named like its series) is an anomaly the ingestion reports, never a legal case.
     *
     * @param normalizedTitle normalized title to look up.
     * @return the single anime carrying this title with its children, or null.
     * @see [AnimeWithChildren]
     */
    abstract fun findByNormalizedTitle(normalizedTitle: String): AnimeWithChildren?

    /**
     * Loads the anime aggregate linked to a streaming platform identifier.
     *
     * @param platform streaming platform owning the link.
     * @param platformId identifier of the anime on the streaming platform.
     * @return the linked anime with its children, or `null` when no anime carries this link.
     * @see [AnimeWithChildren]
     */
    abstract fun findByPlatformAndPlatformId(platform: Platform, platformId: String): AnimeWithChildren?

    /**
     * Persists the anime identity row and returns that row.
     *
     * The anime carries no mutable column, so an already known anime is returned untouched. The
     * children are upserted right after by the caller, which composes the aggregate from those
     * fresh rows: composing them here would query what the caller immediately replaces.
     *
     * @param anime aggregate root to persist; its children are not written here.
     * @return the stored anime row.
     * @see [Anime]
     * @see [AnimeWithChildren]
     */
    abstract fun save(anime: AnimeWithChildren): Anime
}
