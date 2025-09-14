package fr.shikkanime.repositories

import fr.shikkanime.entities.*
import fr.shikkanime.entities.enums.CountryCode
import fr.shikkanime.entities.enums.EpisodeType
import fr.shikkanime.entities.miscellaneous.GroupedEpisode
import fr.shikkanime.entities.miscellaneous.Pageable
import fr.shikkanime.entities.miscellaneous.SortParameter
import jakarta.persistence.EntityManager
import jakarta.persistence.criteria.*
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import java.util.*

class GroupedEpisodeRepository : AbstractRepository<EpisodeMapping>() {
    override fun getEntityClass() = EpisodeMapping::class.java

    /**
     * Represents the unique identifier for a group of episodes.
     * Episodes are grouped by anime, type, the day of release, and a 2-hour block within that day.
     *
     * @property animeUuid The UUID of the anime.
     * @property episodeType The type of the episode (e.g., EPISODE, FILM).
     * @property releaseDay The truncated day of release.
     * @property hourGroup The 2-hour block index (0-11) within the release day.
     * @property representativeReleaseTime The earliest release time within the group, used as a reference.
     */
    private data class GroupIdentifier(
        val animeUuid: UUID,
        val episodeType: EpisodeType,
        val releaseDay: ZonedDateTime,
        val hourGroup: Int,
        val representativeReleaseTime: ZonedDateTime
    )

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

            val pagedGroupIdentifiers = findPagedGroupIdentifiers(entityManager, cb, countryCode, sort, page, limit)

            if (pagedGroupIdentifiers.data.isEmpty()) {
                return@use Pageable(emptySet(), page, limit, 0)
            }

            val variantsInGroups = findVariantsInGroups(entityManager, cb, pagedGroupIdentifiers.data, sort)
            val groupedEpisodes = groupVariants(variantsInGroups, pagedGroupIdentifiers.data, sort)

            Pageable(
                data = groupedEpisodes,
                page = pagedGroupIdentifiers.page,
                limit = pagedGroupIdentifiers.limit,
                total = pagedGroupIdentifiers.total,
            )
        }
    }

    private fun getHourExpression(cb: CriteriaBuilder, releaseDateTime: Expression<ZonedDateTime>): Expression<Int> {
        return if (database.dialect().contains("PostgreSQL", ignoreCase = true)) {
            cb.function("date_part", Int::class.java, cb.literal("hour"), releaseDateTime)
        } else {
            cb.function("HOUR", Int::class.java, releaseDateTime)
        }
    }

    private fun getMinuteExpression(cb: CriteriaBuilder, releaseDateTime: Expression<ZonedDateTime>): Expression<Int> {
        return if (database.dialect().contains("PostgreSQL", ignoreCase = true)) {
            cb.function("date_part", Int::class.java, cb.literal("minute"), releaseDateTime)
        } else {
            cb.function("MINUTE", Int::class.java, releaseDateTime)
        }
    }

    /**
     * Finds the paginated list of group identifiers.
     * Each identifier represents a unique group of episodes for a given page.
     *
     * @param em The EntityManager.
     * @param cb The CriteriaBuilder.
     * @param countryCode Optional country code to filter by.
     * @param sort The sorting parameters.
     * @param page The page number.
     * @param limit The page size.
     * @return A [Pageable] of [GroupIdentifier].
     */
    private fun findPagedGroupIdentifiers(
        em: EntityManager,
        cb: CriteriaBuilder,
        countryCode: CountryCode?,
        sort: List<SortParameter>,
        page: Int,
        limit: Int
    ): Pageable<GroupIdentifier> {
        val query = cb.createQuery(GroupIdentifier::class.java)
        val root = query.from(EpisodeVariant::class.java)
        val mapping = root[EpisodeVariant_.mapping]
        val anime = mapping[EpisodeMapping_.anime]
        val releaseDateTime = root[EpisodeVariant_.releaseDateTime]

        val releaseDay = cb.function("date_trunc", ZonedDateTime::class.java, cb.literal("day"), releaseDateTime)
        val hour = getHourExpression(cb, releaseDateTime)
        val minute = getMinuteExpression(cb, releaseDateTime)
        val minutesOfDay = cb.sum(cb.prod(hour, 60), minute)
        val shiftedMinutes = cb.sum(minutesOfDay, 30)
        val hourGroup = cb.floor(cb.quot(shiftedMinutes, 120)).`as`(Int::class.java)

        query.select(
            cb.construct(
                GroupIdentifier::class.java,
                anime[Anime_.uuid],
                mapping[EpisodeMapping_.episodeType],
                releaseDay,
                hourGroup,
                cb.least(releaseDateTime)
            )
        )

        countryCode?.let { query.where(cb.equal(anime[Anime_.countryCode], it)) }

        query.groupBy(
            anime[Anime_.uuid],
            mapping[EpisodeMapping_.episodeType],
            releaseDay,
            hourGroup
        )

        val groupSortExpression = cb.least(releaseDateTime)
        val sortExpressions = mapOf(SortField.EPISODE_TYPE to mapping[EpisodeMapping_.episodeType])
        val orders = sort.mapNotNull { SortField.toCriteriaOrder(it, cb, groupSortExpression, sortExpressions) }
        query.orderBy(orders)

        return buildPageableQuery(createReadOnlyQuery(em, query), page, limit)
    }

    /**
     * Fetches all [EpisodeVariant] entities that belong to a given set of group identifiers.
     *
     * @param em The EntityManager.
     * @param cb The CriteriaBuilder.
     * @param groups The set of [GroupIdentifier] to fetch variants for.
     * @param sort The sorting parameters to apply to the variants within their groups.
     * @return A list of [EpisodeVariant].
     */
    private fun findVariantsInGroups(
        em: EntityManager,
        cb: CriteriaBuilder,
        groups: Set<GroupIdentifier>,
        sort: List<SortParameter>
    ): List<EpisodeVariant> {
        val query = cb.createQuery(EpisodeVariant::class.java)
        val root = query.from(EpisodeVariant::class.java)
        root.fetch(EpisodeVariant_.mapping, JoinType.INNER).fetch(EpisodeMapping_.anime, JoinType.INNER)

        val mapping = root[EpisodeVariant_.mapping]
        val anime = mapping[EpisodeMapping_.anime]
        val releaseDateTime = root[EpisodeVariant_.releaseDateTime]

        val releaseDay = cb.function("date_trunc", ZonedDateTime::class.java, cb.literal("day"), releaseDateTime)
        val hour = getHourExpression(cb, releaseDateTime)
        val minute = getMinuteExpression(cb, releaseDateTime)
        val minutesOfDay = cb.sum(cb.prod(hour, 60), minute)
        val shiftedMinutes = cb.sum(minutesOfDay, 30)
        val hourGroup = cb.floor(cb.quot(shiftedMinutes, 120))

        val predicates = groups.map {
            cb.and(
                cb.equal(anime[Anime_.uuid], it.animeUuid),
                cb.equal(mapping[EpisodeMapping_.episodeType], it.episodeType),
                cb.equal(releaseDay, it.releaseDay),
                cb.equal(hourGroup, it.hourGroup)
            )
        }

        query.where(cb.or(*predicates.toTypedArray()))

        val sortExpressions = mapOf(
            SortField.EPISODE_TYPE to mapping[EpisodeMapping_.episodeType],
            SortField.SEASON to mapping[EpisodeMapping_.season],
            SortField.NUMBER to mapping[EpisodeMapping_.number],
            SortField.ANIME_NAME to anime[Anime_.slug]
        )
        val orders = sort.mapNotNull { SortField.toCriteriaOrder(it, cb, releaseDateTime, sortExpressions) }
        query.orderBy(orders)

        return createReadOnlyQuery(em, query).resultList
    }

    /**
     * Groups a flat list of variants into [GroupedEpisode] objects based on the provided group identifiers.
     *
     * @param variants The list of [EpisodeVariant] to group.
     * @param groups The set of [GroupIdentifier] defining the groups.
     * @param sort The sorting parameters to apply to the final list of [GroupedEpisode].
     * @return A sorted set of [GroupedEpisode].
     */
    private fun groupVariants(
        variants: List<EpisodeVariant>,
        groups: Set<GroupIdentifier>,
        sort: List<SortParameter>
    ): Set<GroupedEpisode> {
        val groupIdentifierMap = groups.associateBy {
            it.animeUuid to Triple(it.episodeType, it.releaseDay, it.hourGroup)
        }

        return variants.groupBy { variant ->
            val releaseTime = variant.releaseDateTime
            val releaseDay = releaseTime.truncatedTo(ChronoUnit.DAYS)
            val minutesOfDay = releaseTime.hour * 60 + releaseTime.minute
            val shiftedMinutes = minutesOfDay + 30
            val hourGroup = shiftedMinutes / 120

            val groupKey = variant.mapping!!.anime!!.uuid!! to Triple(
                variant.mapping!!.episodeType!!,
                releaseDay,
                hourGroup
            )

            groupIdentifierMap[groupKey]
                ?: throw IllegalStateException("Variant does not belong to any group. Variant release time: ${variant.releaseDateTime}")
        }
            .values
            .map(::toGroupedEpisode)
            .sortedWith(getGroupedEpisodeComparator(sort))
            .toSet()
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

    /**
     * Converts a list of [EpisodeVariant] belonging to the same group into a single [GroupedEpisode].
     *
     * @param variants The list of variants in the group.
     * @return A [GroupedEpisode] object.
     */
    private fun toGroupedEpisode(variants: List<EpisodeVariant>): GroupedEpisode {
        val firstVariant = variants.first()
        val firstMapping = firstVariant.mapping!!

        val mappingUuids = variants.asSequence()
            .map { it.mapping!! }
            .sortedWith(compareBy({ it.season }, { it.episodeType!! }, { it.number }))
            .map { it.uuid!! }
            .toSet()

        val isSingleMapping = mappingUuids.size == 1

        return GroupedEpisode(
            anime = firstMapping.anime!!,
            releaseDateTime = variants.minOf { it.releaseDateTime },
            lastUpdateDateTime = variants.maxOf { it.mapping!!.lastUpdateDateTime },
            minSeason = variants.minOf { it.mapping!!.season!! },
            maxSeason = variants.maxOf { it.mapping!!.season!! },
            episodeType = firstMapping.episodeType!!,
            minNumber = variants.minOf { it.mapping!!.number!! },
            maxNumber = variants.maxOf { it.mapping!!.number!! },
            platforms = variants.map { it.platform!! }.toSet(),
            audioLocales = variants.map { it.audioLocale!! }.toSet(),
            urls = variants.map { it.url!! }.toSet(),
            mappings = mappingUuids,
            title = if (isSingleMapping) firstMapping.title else null,
            description = if (isSingleMapping) firstMapping.description else null,
            duration = if (isSingleMapping) firstMapping.duration else null
        )
    }
}