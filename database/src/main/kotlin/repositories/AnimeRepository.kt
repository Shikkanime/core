package fr.shikkanime.database.repositories

import fr.shikkanime.core.pagination.Page
import fr.shikkanime.core.pagination.PageRequest
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
     * The public API still serves the whole catalog; the administration reads it page by page
     * through [findPage]. Keeping the unbounded read here avoids changing the public contract in a
     * change scoped to the administration.
     *
     * @return every stored anime aggregate, in no particular order.
     * @see [AnimeWithChildren]
     */
    abstract fun findAllModels(): List<AnimeWithChildren>

    /**
     * Loads one page of the anime catalog with its children.
     *
     * The page is read in broadcast order, by normalized title, and composed with one batch read
     * per relation: the database only returns the rows the administration displays.
     *
     * @param request page and size to read.
     * @return the requested page of anime aggregates.
     * @see [AnimeWithChildren]
     */
    abstract fun findPage(request: PageRequest): Page<AnimeWithChildren>

    /**
     * Loads one page of the anime aggregates matching a search term.
     *
     * The match runs in the database, never in the client: the identity title and every localized
     * title are compared case-insensitively, and a term that is a UUID also matches the identifier.
     * Filtering in memory would read the whole catalog to answer one query.
     *
     * @param search term typed by the administrator, matched as a substring.
     * @param request page and size to read.
     * @return the requested page of matching anime aggregates.
     * @see [AnimeWithChildren]
     */
    abstract fun findPageBySearch(search: String, request: PageRequest): Page<AnimeWithChildren>

    /**
     * Loads one page of the anime aggregates linked to a streaming platform.
     *
     * The platform is applied by the database, alongside the optional search term: a platform page
     * never reads the whole catalog to filter it in memory, and a platform without any anime simply
     * answers an empty page.
     *
     * @param platform streaming platform the anime must be linked to.
     * @param search optional term matched like [findPageBySearch], or `null` to keep every row.
     * @param request page and size to read.
     * @return the requested page of matching anime aggregates.
     * @see [AnimeWithChildren]
     */
    abstract fun findPageByPlatform(
        platform: Platform,
        search: String?,
        request: PageRequest
    ): Page<AnimeWithChildren>

    /**
     * Loads the anime rows carrying these identifiers.
     *
     * One grouped read answers a whole page of display rows, which is what keeps a list of episodes
     * from resolving its anime one row at a time.
     *
     * @param ids identifiers of the animes to read.
     * @return the matching anime rows indexed by identifier, empty when [ids] is empty.
     * @see [Anime]
     */
    abstract fun findModelsByIds(ids: List<Uuid>): Map<Uuid, Anime>

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
     * @param anime anime row to persist; its children are not written here.
     * @return the stored anime row.
     * @see [Anime]
     */
    abstract fun save(anime: Anime): Anime
}
