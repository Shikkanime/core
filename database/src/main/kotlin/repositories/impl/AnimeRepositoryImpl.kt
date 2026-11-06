package fr.shikkanime.database.repositories.impl

import fr.shikkanime.core.pagination.Page
import fr.shikkanime.core.pagination.PageRequest
import fr.shikkanime.database.entities.AnimeEntity
import fr.shikkanime.database.entities.AnimeLocalizationTable
import fr.shikkanime.database.entities.AnimePlatformTable
import fr.shikkanime.database.entities.AnimeTable
import fr.shikkanime.database.repositories.AnimeLocalizationRepository
import fr.shikkanime.database.repositories.AnimePlatformRepository
import fr.shikkanime.database.repositories.AnimeRepository
import fr.shikkanime.database.repositories.ImageRepository
import fr.shikkanime.exposed.pagination.paginate
import fr.shikkanime.models.Anime
import fr.shikkanime.models.AnimeWithChildren
import fr.shikkanime.models.Platform
import org.jetbrains.exposed.v1.core.Op
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.core.like
import org.jetbrains.exposed.v1.core.lowerCase
import org.jetbrains.exposed.v1.core.or
import org.jetbrains.exposed.v1.jdbc.select
import org.koin.core.annotation.Single
import kotlin.uuid.Uuid

@Single(binds = [AnimeRepository::class])
internal class AnimeRepositoryImpl(
    private val imageRepository: ImageRepository,
    // The children are injected lazily: they reference this repository to mint their foreign keys
    private val localizationRepository: Lazy<AnimeLocalizationRepository>,
    private val platformRepository: Lazy<AnimePlatformRepository>
) : AnimeRepository() {

    /**
     * Replaces the entities of a page with their aggregates, keeping its bounds.
     *
     * @param aggregates aggregates composed from the entities of this page.
     * @return the same page holding the aggregates.
     */
    private fun Page<AnimeEntity>.withAggregates(aggregates: List<AnimeWithChildren>): Page<AnimeWithChildren> =
        Page(aggregates, page, size, totalElements)

    /**
     * Composes the aggregate view of the given rows from one batch read per relation.
     *
     * The image table is shared by every entity and cannot be reached by a relation, and the
     * relatable children are loaded as whole groups too: one lookup per row is the banned N+1.
     *
     * @param animes entity rows to compose.
     * @return the aggregates of the given rows, children included.
     * @see [AnimeWithChildren]
     */
    private fun compose(animes: List<AnimeEntity>): List<AnimeWithChildren> {
        // Nothing to compose: skip the relation reads instead of querying empty id lists
        if (animes.isEmpty()) return emptyList()

        val ids = animes.map { it.id.value }

        // Resolve the artwork of the whole page in one grouped read
        val images = imageRepository.findAllByEntityIds(ids)

        // Resolve the localized texts of the whole page in one grouped read
        val localizations = localizationRepository.value.findAllByAnimeIds(ids)

        // Resolve the platform source links of the whole page in one grouped read
        val platforms = platformRepository.value.findAllByAnimeIds(ids)

        return animes.map {
            AnimeWithChildren(
                anime = it.toModel(),
                images = images[it.id.value].orEmpty(),
                localizations = localizations[it.id.value].orEmpty(),
                platforms = platforms[it.id.value].orEmpty()
            )
        }
    }

    /**
     * Composes the aggregate view of one row from one batch read per relation.
     *
     * The children cannot be reached from the row alone: the shared artwork table and the
     * relationless children are resolved here, one whole group at a time.
     *
     * @param entity entity row to compose.
     * @return the aggregate of the given row, children included.
     * @see [AnimeWithChildren]
     */
    private fun composeOne(entity: AnimeEntity): AnimeWithChildren =
        compose(listOf(entity))
            .single()

    /**
     * Builds the predicate matching a free search term.
     *
     * The term is compared to the identity title and to every localized title, case-insensitively;
     * a term that parses as an identifier also matches the row identifier.
     *
     * @param search term typed by the administrator, matched as a substring.
     * @return the predicate a query applies to keep the matching rows.
     */
    private fun searchCondition(search: String): Op<Boolean> {
        val term = "%${search.trim().lowercase()}%"
        val identifier = runCatching { Uuid.parse(search.trim()) }.getOrNull()

        val conditions: MutableList<Op<Boolean>> = mutableListOf(
            AnimeTable.normalizedTitle.lowerCase() like term,
            AnimeLocalizationTable.title.lowerCase() like term
        )

        if (identifier != null) {
            conditions += AnimeTable.id eq identifier
        }

        return conditions.reduce { left, right -> left or right }
    }

    override fun findAllModels(): List<AnimeWithChildren> =
        compose(
            super.findAll()
                .toList()
        )

    override fun findPage(request: PageRequest, search: String?): Page<AnimeWithChildren> {
        val query = AnimeTable.leftJoin(AnimeLocalizationTable)
            .select(AnimeTable.columns)
            .withDistinct(true)

        if (!search.isNullOrBlank())
            query.where { searchCondition(search) }

        val page = AnimeEntity.wrapRows(query)
            .orderBy(AnimeTable.normalizedTitle.lowerCase() to SortOrder.ASC)
            .paginate(request)

        return page.withAggregates(compose(page.data))
    }

    override fun findModelsByIds(ids: List<Uuid>): Map<Uuid, Anime> =
        if (ids.isEmpty()) {
            emptyMap()
        } else {
            AnimeEntity.find { AnimeTable.id inList ids }
                .associate { it.id.value to it.toModel() }
        }

    override fun findAllByNormalizedTitles(titles: List<String>): Map<String, AnimeWithChildren> =
        compose(
            AnimeEntity.find { AnimeTable.normalizedTitle inList titles }
                .toList()
        )
            .associateBy { it.anime.normalizedTitle }

    override fun findModelById(id: Uuid): AnimeWithChildren? =
        findById(id)
            ?.let(::composeOne)

    override fun findByNormalizedTitle(normalizedTitle: String): AnimeWithChildren? =
        AnimeEntity.find { AnimeTable.normalizedTitle eq normalizedTitle }
            .limit(1)
            .firstOrNull()
            ?.let(::composeOne)

    override fun findByPlatformAndPlatformId(platform: Platform, platformId: String): AnimeWithChildren? =
        AnimeEntity.wrapRows(
            AnimeTable.innerJoin(AnimePlatformTable)
                .select(AnimeTable.columns)
                .where {
                    (AnimePlatformTable.platform eq platform) and
                        (AnimePlatformTable.platformId eq platformId)
                }
                .limit(1)
        )
            .firstOrNull()
            ?.let(::composeOne)

    override fun save(anime: Anime): Anime =
        anime.id?.let(::findById)
            .newIfNotExists(anime.id) {
                normalizedTitle = anime.normalizedTitle
            }
            .applyFlush()
            .toModel()
}
