package fr.shikkanime.repositories

import com.google.inject.Inject
import fr.shikkanime.entities.*
import fr.shikkanime.entities.enums.CountryCode
import fr.shikkanime.entities.miscellaneous.GroupedEpisode
import fr.shikkanime.entities.miscellaneous.Pageable
import fr.shikkanime.entities.miscellaneous.SortParameter
import fr.shikkanime.factories.impl.GroupedEpisodeFactory
import fr.shikkanime.utils.indexers.DeprecatedGroupedIndexer
import fr.shikkanime.utils.indexers.ReverseIndexedRecord
import jakarta.persistence.EntityManager
import jakarta.persistence.criteria.*
import java.time.ZonedDateTime
import java.util.*

class GroupedEpisodeRepository : AbstractRepository<EpisodeMapping>() {
    @Inject private lateinit var groupedEpisodeFactory: GroupedEpisodeFactory

    override fun getEntityClass() = EpisodeMapping::class.java

    /**
     * Defines the available fields for sorting grouped episodes.
     * It maps API-facing field names to database expressions.
     *
     * @property fieldName The name of the field as exposed in the API.
     */
    private enum class SortField(val fieldName: String) {
        RELEASE_DATE_TIME("releaseDateTime"),
        EPISODE_TYPE("episodeType"),
        SEASON("season"),
        NUMBER("number"),
        ANIME_NAME("animeName");

        companion object {
            /**
             * Finds a [SortField] entry from its field name.
             *
             * @param field The field name string.
             * @return The corresponding [SortField] or null if not found.
             */
            fun from(field: String) = entries.find { it.fieldName == field }

            /**
             * Converts a [SortParameter] into a Criteria API [Order] object.
             *
             * @param param The sort parameter from the API.
             * @param cb The CriteriaBuilder.
             * @param groupSortExpression The expression to use for sorting by release date.
             * @param variantSortExpressions A map of expressions for other sortable fields.
             * @return An [Order] object or null if the field is not sortable.
             */
            fun toCriteriaOrder(
                param: SortParameter,
                cb: CriteriaBuilder,
                groupSortExpression: Expression<ZonedDateTime>,
                variantSortExpressions: Map<SortField, Path<*>>
            ): Order? {
                val path = when (from(param.field)) {
                    RELEASE_DATE_TIME -> groupSortExpression
                    EPISODE_TYPE -> variantSortExpressions[EPISODE_TYPE]
                    SEASON -> variantSortExpressions[SEASON]
                    NUMBER -> variantSortExpressions[NUMBER]
                    ANIME_NAME -> variantSortExpressions[ANIME_NAME]
                    else -> null
                } ?: return null

                return if (param.order == SortParameter.Order.ASC) cb.asc(path) else cb.desc(path)
            }
        }
    }

    /**
     * Finds and groups episodes into a paginated result.
     * The process involves three main steps:
     * 1. Find the identifiers of the groups for the requested page.
     * 2. Fetch all episode variants belonging to these groups.
     * 3. Group the variants in memory and map them to [GroupedEpisode] objects.
     *
     * @param countryCode The country code to filter animes by. Can be null.
     * @param sort A list of [SortParameter] to apply.
     * @param page The page number to retrieve.
     * @param limit The number of items per page.
     * @return A [Pageable] containing the set of [GroupedEpisode].
     */
    fun findAllBy(countryCode: CountryCode?, sort: List<SortParameter>, page: Int, limit: Int): Pageable<GroupedEpisode> {
        return database.entityManager.use { entityManager ->
            val cb = entityManager.criteriaBuilder
            val pagedGroupIdentifiers = paginateGroupedEpisodes(countryCode, sort, page, limit)

            if (pagedGroupIdentifiers.data.isEmpty()) {
                return@use Pageable(emptySet(), page, limit, 0)
            }

            val variantsInGroups = findVariantsInGroups(entityManager, cb, pagedGroupIdentifiers.data.flatMap { record -> record.first.value.map { it.uuid } }.toSet(), sort)
            val groupedEpisodes = groupVariants(variantsInGroups, pagedGroupIdentifiers.data, sort)

            Pageable(
                data = groupedEpisodes,
                page = pagedGroupIdentifiers.page,
                limit = pagedGroupIdentifiers.limit,
                total = pagedGroupIdentifiers.total,
            )
        }
    }

    private fun paginateGroupedEpisodes(
        countryCode: CountryCode?,
        sort: List<SortParameter>,
        page: Int,
        limit: Int
    ): Pageable<ReverseIndexedRecord> = DeprecatedGroupedIndexer.pageableRecords(
        filter = { (_, compositeIndex) -> countryCode == null || compositeIndex.countryCode == countryCode },
        comparator = Comparator { a, b ->
            for (param in sort) {
                val comparison = when (SortField.from(param.field)) {
                    SortField.RELEASE_DATE_TIME -> a.first.key.compareTo(b.first.key)
                    SortField.EPISODE_TYPE -> a.second.episodeType.compareTo(b.second.episodeType)
                    SortField.ANIME_NAME -> a.second.animeSlug.compareTo(b.second.animeSlug)
                    else -> 0
                }
                if (comparison != 0) {
                    return@Comparator if (param.order == SortParameter.Order.ASC) comparison else -comparison
                }
            }
            0
        },
        page = page,
        limit = limit,
    )

    /**
     * Fetches all [EpisodeVariant] entities that belong to a given set of group identifiers.
     *
     * @param em The EntityManager.
     * @param cb The CriteriaBuilder.
     * @param uuids The set of group identifiers to fetch variants for.
     * @param sort The sorting parameters to apply to the variants within their groups.
     * @return A list of [EpisodeVariant].
     */
    private fun findVariantsInGroups(
        em: EntityManager,
        cb: CriteriaBuilder,
        uuids: Set<UUID>,
        sort: List<SortParameter>
    ): List<EpisodeVariant> {
        val query = cb.createQuery(EpisodeVariant::class.java)
        val root = query.from(EpisodeVariant::class.java)
        root.fetch(EpisodeVariant_.mapping, JoinType.INNER).fetch(EpisodeMapping_.anime, JoinType.INNER)

        val mapping = root[EpisodeVariant_.mapping]
        val anime = mapping[EpisodeMapping_.anime]

        query.where(root[EpisodeVariant_.uuid].`in`(uuids))

        val sortExpressions = mapOf(
            SortField.EPISODE_TYPE to mapping[EpisodeMapping_.episodeType],
            SortField.SEASON to mapping[EpisodeMapping_.season],
            SortField.NUMBER to mapping[EpisodeMapping_.number],
            SortField.ANIME_NAME to anime[Anime_.slug]
        )

        val orders = sort.mapNotNull { SortField.toCriteriaOrder(it, cb, root[EpisodeVariant_.releaseDateTime], sortExpressions) }
        query.orderBy(orders)

        return createReadOnlyQuery(em, query).resultList
    }

    /**
     * Groups a flat list of variants into [GroupedEpisode] objects based on the provided group identifiers.
     *
     * @param variants The list of [EpisodeVariant] to group.
     * @param groupIdentifiers The identifiers of the groups to form.
     * @param sort The sorting parameters to apply to the final list of [GroupedEpisode].
     * @return A sorted set of [GroupedEpisode].
     */
    private fun groupVariants(
        variants: List<EpisodeVariant>,
        groupIdentifiers: Set<ReverseIndexedRecord>,
        sort: List<SortParameter>
    ): Set<GroupedEpisode> {
        return variants.groupBy { variant -> groupIdentifiers.find { it.first.value.any { data -> data.uuid == variant.uuid } }!! }.values
            .map(groupedEpisodeFactory::toEntity)
            .toSortedSet(getGroupedEpisodeComparator(sort))
    }


    /**
     * Creates a comparator to sort [GroupedEpisode] objects based on the API sort parameters.
     * This is used for in-memory sorting after the database query.
     *
     * @param sort The list of [SortParameter].
     * @return A [Comparator] for [GroupedEpisode].
     */
    private fun getGroupedEpisodeComparator(sort: List<SortParameter>): Comparator<GroupedEpisode> {
        return Comparator { a, b ->
            for (param in sort) {
                val comparison = when (SortField.from(param.field)) {
                    SortField.RELEASE_DATE_TIME -> a.releaseDateTime.compareTo(b.releaseDateTime)
                    SortField.EPISODE_TYPE -> a.episodeType.compareTo(b.episodeType)
                    SortField.SEASON -> a.minSeason.compareTo(b.minSeason)
                    SortField.NUMBER -> a.minNumber.compareTo(b.minNumber)
                    SortField.ANIME_NAME -> a.anime.slug!!.compareTo(b.anime.slug!!)
                    else -> 0
                }
                if (comparison != 0) {
                    return@Comparator if (param.order == SortParameter.Order.ASC) comparison else -comparison
                }
            }
            0
        }
    }
}