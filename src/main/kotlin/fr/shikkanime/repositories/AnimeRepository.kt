package fr.shikkanime.repositories

import fr.shikkanime.entities.*
import fr.shikkanime.entities.enums.CountryCode
import fr.shikkanime.entities.enums.LangType
import fr.shikkanime.entities.enums.Platform
import fr.shikkanime.entities.miscellaneous.Pageable
import fr.shikkanime.entities.miscellaneous.Season
import fr.shikkanime.entities.miscellaneous.SortParameter
import jakarta.persistence.EntityManager
import jakarta.persistence.criteria.*
import org.hibernate.Hibernate
import org.hibernate.search.mapper.orm.Search
import java.time.ZonedDateTime
import java.util.*

class AnimeRepository : AbstractRepository<Anime>() {
    override fun getEntityClass() = Anime::class.java

    fun preIndex() {
        database.entityManager.use {
            val searchSession = Search.session(it)
            val indexer = searchSession.massIndexer(getEntityClass())
            indexer.startAndWait()
        }
    }

    private data class SearchContext(
        val countryCode: CountryCode?,
        val simulcastUuid: UUID?,
        val searchTypes: Array<LangType>?,
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is SearchContext) return false

            if (countryCode != other.countryCode) return false
            if (simulcastUuid != other.simulcastUuid) return false
            if (!searchTypes.contentEquals(other.searchTypes)) return false

            return true
        }

        override fun hashCode(): Int {
            var result = countryCode?.hashCode() ?: 0
            result = 31 * result + (simulcastUuid?.hashCode() ?: 0)
            result = 31 * result + (searchTypes?.contentHashCode() ?: 0)
            return result
        }
    }

    private fun applyFilter(
        criteriaBuilder: CriteriaBuilder,
        root: Root<Anime>,
        searchContext: SearchContext,
    ): List<Predicate> {
        return buildList {
            searchContext.simulcastUuid?.let {
                val simulcastJoin = root.join(Anime_.simulcasts)
                add(criteriaBuilder.equal(simulcastJoin[Simulcast_.uuid], it))
            }

            searchContext.countryCode?.let { cc ->
                add(criteriaBuilder.equal(root[Anime_.countryCode], cc))

                searchContext.searchTypes?.let { types ->
                    val variantJoin = root.join(Anime_.mappings, JoinType.LEFT)
                        .join(EpisodeMapping_.variants, JoinType.LEFT)

                    add(
                        criteriaBuilder.or(
                            *types.map { langType ->
                                when (langType) {
                                    LangType.SUBTITLES -> criteriaBuilder.notEqual(variantJoin[EpisodeVariant_.audioLocale], cc.locale)
                                    LangType.VOICE -> criteriaBuilder.equal(variantJoin[EpisodeVariant_.audioLocale], cc.locale)
                                }
                            }.toTypedArray()
                        )
                    )
                }
            }
        }
    }

    fun findAllBy(
        countryCode: CountryCode?,
        simulcastUuid: UUID?,
        name: String? = null,
        searchTypes: Array<LangType>?,
        sort: List<SortParameter>,
        page: Int,
        limit: Int,
    ): Pageable<Anime> {
        val searchContext = SearchContext(countryCode, simulcastUuid, searchTypes)

        return database.entityManager.use { entityManager ->
            if (!name.isNullOrBlank() && name.length > 1) {
                findByFullTextSearch(entityManager, searchContext, name, page, limit)
            } else {
                findByStandardQuery(entityManager, searchContext, name, sort, page, limit)
            }
        }
    }

    private fun findByFullTextSearch(
        entityManager: EntityManager,
        searchContext: SearchContext,
        name: String,
        page: Int,
        limit: Int,
    ): Pageable<Anime> {
        val searchResults = executeFullTextSearch(entityManager, name)
        if (searchResults.isEmpty()) {
            return Pageable(emptySet(), page, limit, 0)
        }

        val cb = entityManager.criteriaBuilder
        val countQuery = cb.createQuery(UUID::class.java)
        val root = countQuery.from(getEntityClass())

        val predicates = buildList<Predicate> {
            add(root[Anime_.uuid].`in`(searchResults.keys))
            addAll(applyFilter(cb, root, searchContext))
        }

        countQuery.distinct(true)
            .select(root[Anime_.uuid])
            .where(*predicates.toTypedArray())

        val filteredIds = createReadOnlyQuery(entityManager, countQuery).resultList.toList()
        val sortedPagedIds = filteredIds
            .sortedByDescending { uuid -> searchResults[uuid] }
            .drop((page - 1) * limit)
            .take(limit)

        if (sortedPagedIds.isEmpty()) {
            return Pageable(emptySet(), page, limit, filteredIds.size.toLong())
        }

        return Pageable(
            findAllByUuids(sortedPagedIds)
                .sortedBy { anime -> sortedPagedIds.indexOf(anime.uuid) }
                .toSet(),
            page,
            limit,
            filteredIds.size.toLong()
        )
    }

    private fun executeFullTextSearch(entityManager: EntityManager, name: String): Map<UUID, Float> {
        val searchSession = Search.session(entityManager)
        return searchSession.search(getEntityClass())
            .select { s -> s.composite(s.id(), s.score()) }
            .where { w -> w.bool().must(w.match().field(Anime_.NAME).matching(name)) }
            .fetchAll().hits()
            .filterIsInstance<List<Any>>()
            .associate { array -> (array[0] as UUID) to (array[1] as Float) }
    }

    private fun findByStandardQuery(
        entityManager: EntityManager,
        searchContext: SearchContext,
        name: String?,
        sort: List<SortParameter>,
        page: Int,
        limit: Int,
    ): Pageable<Anime> {
        val cb = entityManager.criteriaBuilder
        val query = cb.createQuery(getEntityClass())
        val root = query.from(getEntityClass())
        query.distinct(true).select(root)

        val predicates = buildList {
            addAll(applyFilter(cb, root, searchContext))

            if (!name.isNullOrBlank()) {
                add(
                    cb.equal(
                    cb.function(
                        "REGEXP_REPLACE", String::class.java,
                        cb.upper(cb.substring(root[Anime_.slug], 1, 1)),
                        cb.literal("[^A-Z]"),
                        cb.literal("#"),
                    ),
                        name.uppercase()
                    )
                )
            }
        }

        query.where(*predicates.toTypedArray())

        val orders = buildSortOrders(sort, cb, root, name)
        query.orderBy(orders)

        return buildPageableQuery(createReadOnlyQuery(entityManager, query), page, limit)
    }

    private fun buildSortOrders(
        sort: List<SortParameter>,
        cb: CriteriaBuilder,
        root: Root<Anime>,
        name: String?,
    ): List<Order> {
        val orders = sort.mapNotNull { sortParameter ->
            val field = when (sortParameter.field) {
                "name" -> root[Anime_.slug]
                "releaseDateTime" -> root[Anime_.releaseDateTime]
                "lastReleaseDateTime" -> root[Anime_.lastReleaseDateTime]
                else -> null
            }
            field?.let { if (sortParameter.order == SortParameter.Order.ASC) cb.asc(it) else cb.desc(it) }
        }.toMutableList()

        if (orders.isEmpty() && !name.isNullOrBlank()) {
            orders.add(cb.asc(root[Anime_.slug]))
        }

        return orders
    }

    fun findAllBySimulcast(simulcastUuid: UUID): List<Anime> {
        return database.entityManager.use {
            val cb = it.criteriaBuilder
            val query = cb.createQuery(getEntityClass())
            val root = query.from(getEntityClass())
            val simulcastJoin = root.join(Anime_.simulcasts)

            query.where(cb.equal(simulcastJoin[Simulcast_.uuid], simulcastUuid))
                .orderBy(cb.asc(root[Anime_.slug]))

            createReadOnlyQuery(it, query)
                .resultList
        }
    }

    fun findAllNeedUpdate(
        currentSimulcastUuid: UUID?,
        lastSimulcastUuid: UUID?,
        currentSeasonDelay: Long,
        lastSeasonDelay: Long,
        othersDelay: Long
    ): List<Anime> {
        return database.entityManager.use {
            val cb = it.criteriaBuilder
            val query = cb.createQuery(getEntityClass())
            val root = query.from(getEntityClass())
            val simulcastJoin = root.join(Anime_.simulcasts, JoinType.LEFT)
            val now = ZonedDateTime.now()

            val predicates = buildSet {
                currentSimulcastUuid?.let { uuid ->
                    add(cb.and(
                        cb.equal(simulcastJoin[Simulcast_.uuid], uuid),
                        cb.lessThanOrEqualTo(root[Anime_.lastUpdateDateTime], now.minusDays(currentSeasonDelay))
                    ))
                }
                lastSimulcastUuid?.let { uuid ->
                    add(cb.and(
                        cb.equal(simulcastJoin[Simulcast_.uuid], uuid),
                        cb.lessThanOrEqualTo(root[Anime_.lastUpdateDateTime], now.minusDays(lastSeasonDelay))
                    ))
                }
                add(cb.and(
                    cb.not(root[Anime_.uuid].`in`(query.subquery(UUID::class.java).apply {
                        val subRoot = from(Anime::class.java)
                        select(subRoot[Anime_.uuid])
                            .where(subRoot.join(Anime_.simulcasts)[Simulcast_.uuid]
                                .`in`(listOfNotNull(currentSimulcastUuid, lastSimulcastUuid)))
                    })),
                    cb.lessThanOrEqualTo(root[Anime_.lastUpdateDateTime], now.minusDays(othersDelay))
                ))
            }

            query.where(
                cb.and(
                    cb.or(*predicates.toTypedArray()),
                    cb.isNotEmpty(root[Anime_.platformIds])
                )
            ).orderBy(cb.asc(root[Anime_.lastUpdateDateTime]))
                .distinct(true)

            createReadOnlyQuery(it, query).resultList
        }
    }


    fun findAllAudioLocales(uuid: UUID): List<String> {
        return database.entityManager.use {
            val cb = it.criteriaBuilder
            val query = cb.createQuery(String::class.java)
            val root = query.from(EpisodeVariant::class.java)

            query.distinct(true)
                .select(root[EpisodeVariant_.audioLocale])
                .where(cb.equal(root[EpisodeVariant_.mapping][EpisodeMapping_.anime][Anime_.uuid], uuid))

            createReadOnlyQuery(it, query)
                .resultList
        }
    }

    fun findAllSeasons(uuid: UUID): List<Season> {
        return database.entityManager.use {
            val cb = it.criteriaBuilder
            val query = cb.createQuery(Season::class.java)
            val root = query.from(getEntityClass())
            val mappingJoin = root.join(Anime_.mappings)

            query.select(
                cb.construct(
                    Season::class.java,
                    mappingJoin[EpisodeMapping_.season],
                    cb.least(mappingJoin[EpisodeMapping_.releaseDateTime]),
                    cb.greatest(mappingJoin[EpisodeMapping_.lastReleaseDateTime]),
                    cb.count(mappingJoin[EpisodeMapping_.uuid])
                )
            ).where(cb.equal(mappingJoin[EpisodeMapping_.anime][Anime_.uuid], uuid))
                .groupBy(mappingJoin[EpisodeMapping_.season])
                .orderBy(cb.asc(mappingJoin[EpisodeMapping_.season]))

            createReadOnlyQuery(it, query)
                .resultList
        }
    }

    fun findAllSimulcastedWithAnimePlatformInvalid(simulcastUuids: Collection<UUID>, platform: Platform, lastValidateDateTime: ZonedDateTime, ignoreAudioLocale: String): List<Anime> {
        return database.entityManager.use {
            val cb = it.criteriaBuilder
            val query = cb.createQuery(getEntityClass())
            val root = query.from(getEntityClass())

            val simulcastJoin = root.join(Anime_.simulcasts, JoinType.INNER)
            val animePlatformJoin = root.join(Anime_.platformIds, JoinType.LEFT)
            animePlatformJoin.on(cb.equal(animePlatformJoin[AnimePlatform_.platform], platform))
            val mappingJoin = root.join(Anime_.mappings, JoinType.INNER)
            val variantJoin = mappingJoin.join(EpisodeMapping_.variants, JoinType.INNER)

            query.distinct(true)
                .select(root)
                .where(cb.and(
                    simulcastJoin[Simulcast_.uuid].`in`(simulcastUuids),
                    cb.or(
                        cb.isNull(animePlatformJoin),
                        cb.lessThanOrEqualTo(animePlatformJoin[AnimePlatform_.lastValidateDateTime], lastValidateDateTime)
                    ),
                    cb.notEqual(variantJoin[EpisodeVariant_.audioLocale], ignoreAudioLocale)
                ))

            createReadOnlyQuery(it, query).resultList
        }
    }

    fun findAllSlugs(): List<String> {
        return database.entityManager.use {
            val cb = it.criteriaBuilder
            val query = cb.createQuery(String::class.java)
            val root = query.from(getEntityClass())

            query.select(root[Anime_.slug])

            createReadOnlyQuery(it, query)
                .resultList
        }
    }

    override fun find(uuid: UUID): Anime? {
        return database.entityManager.use {
            val cb = it.criteriaBuilder
            val query = cb.createQuery(getEntityClass())
            val root = query.from(getEntityClass())

            query.where(cb.equal(root[Anime_.uuid], uuid))

            createReadOnlyQuery(it, query)
                .resultList
                .firstOrNull()
                ?.apply { Hibernate.initialize(simulcasts) }
        }
    }

    fun findBySlug(countryCode: CountryCode, slug: String): Anime? {
        return database.entityManager.use {
            val cb = it.criteriaBuilder
            val query = cb.createQuery(getEntityClass())
            val root = query.from(getEntityClass())

            query.where(
                cb.and(
                    cb.equal(root[Anime_.countryCode], countryCode),
                    cb.equal(root[Anime_.slug], slug)
                )
            )

            createReadOnlyQuery(it, query)
                .resultList
                .firstOrNull()
                ?.apply { Hibernate.initialize(simulcasts) }
        }
    }

    fun findByName(countryCode: CountryCode, name: String?): Anime? {
        return database.entityManager.use {
            val cb = it.criteriaBuilder
            val query = cb.createQuery(getEntityClass())
            val root = query.from(getEntityClass())

            query.where(
                cb.and(
                    cb.equal(root[Anime_.countryCode], countryCode),
                    cb.equal(cb.lower(root[Anime_.name]), name?.lowercase())
                )
            )

            createReadOnlyQuery(it, query)
                .resultList
                .firstOrNull()
        }
    }

    fun updateAllReleaseDate() {
        database.inTransaction {
            val cb = it.criteriaBuilder
            val update = cb.createCriteriaUpdate(getEntityClass())
            val root = update.from(getEntityClass())

            val subQueryMin = update.subquery(ZonedDateTime::class.java)
            val subRootMin = subQueryMin.from(EpisodeMapping::class.java)
            subQueryMin.select(cb.least(subRootMin[EpisodeMapping_.releaseDateTime]))
            subQueryMin.where(cb.equal(subRootMin[EpisodeMapping_.anime], root))

            val subQueryMax = update.subquery(ZonedDateTime::class.java)
            val subRootMax = subQueryMax.from(EpisodeMapping::class.java)
            subQueryMax.select(cb.greatest(subRootMax[EpisodeMapping_.releaseDateTime]))
            subQueryMax.where(cb.equal(subRootMax[EpisodeMapping_.anime], root))

            update[root[Anime_.releaseDateTime]] = subQueryMin
            update[root[Anime_.lastReleaseDateTime]] = subQueryMax

            it.createQuery(update).executeUpdate()
        }
    }
}