package fr.shikkanime.repositories

import fr.shikkanime.entities.*
import fr.shikkanime.entities.enums.CountryCode
import fr.shikkanime.entities.enums.LangType
import fr.shikkanime.entities.miscellaneous.Pageable
import fr.shikkanime.entities.miscellaneous.Season
import fr.shikkanime.entities.miscellaneous.SortParameter
import jakarta.persistence.criteria.CriteriaBuilder
import jakarta.persistence.criteria.JoinType
import jakarta.persistence.criteria.Predicate
import jakarta.persistence.criteria.Root
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

    fun findAllBy(
        countryCode: CountryCode?,
        simulcast: Simulcast?,
        sort: List<SortParameter>,
        page: Int,
        limit: Int,
        searchTypes: Array<LangType>?,
    ): Pageable<Anime> {
        return database.entityManager.use { entityManager ->
            val cb = entityManager.criteriaBuilder
            val query = cb.createQuery(getEntityClass())
            val root = query.from(getEntityClass())
            query.distinct(true).select(root)

            val predicates = mutableListOf<Predicate>()
            simulcast?.let { predicates.add(cb.equal(root.join(Anime_.simulcasts), it)) }
            val orPredicate = predicates(countryCode, predicates, cb, root, searchTypes)

            query.where(
                *predicates.toTypedArray(),
                if (orPredicate.isNotEmpty()) cb.or(*orPredicate.toTypedArray()) else cb.conjunction()
            )

            val orders = sort.mapNotNull { sortParameter ->
                val field = when (sortParameter.field) {
                    "name" -> root[Anime_.slug]
                    "releaseDateTime" -> root[Anime_.releaseDateTime]
                    "lastReleaseDateTime" -> root[Anime_.lastReleaseDateTime]
                    else -> null
                }

                field?.let { if (sortParameter.order == SortParameter.Order.ASC) cb.asc(it) else cb.desc(it) }
            }

            query.orderBy(orders)
            buildPageableQuery(createReadOnlyQuery(entityManager, query), page, limit)
        }
    }

    private fun predicates(
        countryCode: CountryCode?,
        predicates: MutableList<Predicate>,
        cb: CriteriaBuilder,
        root: Root<Anime>,
        searchTypes: Array<LangType>?
    ): List<Predicate> {
        val orPredicate = mutableListOf<Predicate>()

        countryCode?.let { cc ->
            predicates.add(cb.equal(root[Anime_.countryCode], cc))

            searchTypes?.let { st ->
                val mappingJoin = root.join(Anime_.mappings, JoinType.LEFT)
                val variantJoin = mappingJoin.join(EpisodeMapping_.variants, JoinType.LEFT)

                st.forEach { langType ->
                    when (langType) {
                        LangType.SUBTITLES -> orPredicate.add(
                            cb.notEqual(
                                variantJoin[EpisodeVariant_.audioLocale],
                                cc.locale
                            )
                        )

                        LangType.VOICE -> orPredicate.add(cb.equal(variantJoin[EpisodeVariant_.audioLocale], cc.locale))
                    }
                }
            }
        }

        return orPredicate
    }

    fun findAllByName(
        countryCode: CountryCode?,
        name: String,
        page: Int,
        limit: Int,
        searchTypes: Array<LangType>?
    ): Pageable<Anime> {
        return database.entityManager.use { entityManager ->
            val searchResults = (Search.session(entityManager).search(getEntityClass())
                .select { s -> s.composite(s.id(), s.score()) }
                .where { w -> w.bool().must(w.match().field(Anime_.NAME).matching(name)) }
                .fetchAll().hits().filterIsInstance<List<Any>>())
                .associate { array -> (array[0] as UUID) to (array[1] as Float) }

            if (searchResults.isEmpty()) {
                return@use Pageable(emptySet(), page, limit, 0)
            }

            val cb = entityManager.criteriaBuilder
            val countQuery = cb.createQuery(UUID::class.java)
            val root = countQuery.from(getEntityClass())

            val queryPredicates = mutableListOf<Predicate>(root[Anime_.uuid].`in`(searchResults.keys))
            val orPredicate = predicates(countryCode, queryPredicates, cb, root, searchTypes)

            countQuery.distinct(true)
                .select(root[Anime_.uuid]).where(
                    *queryPredicates.toTypedArray(),
                    if (orPredicate.isNotEmpty()) cb.or(*orPredicate.toTypedArray()) else cb.conjunction()
                )

            val filteredIds = entityManager.createQuery(countQuery).resultList.toList()

            val sortedPagedIds = filteredIds
                .sortedByDescending { uuid -> searchResults[uuid] }
                .drop((page - 1) * limit)
                .take(limit)

            if (sortedPagedIds.isEmpty()) {
                return@use Pageable(emptySet(), page, limit, filteredIds.size.toLong())
            }

            val entityQuery = cb.createQuery(getEntityClass())
            val entityRoot = entityQuery.from(getEntityClass())
            entityQuery.where(entityRoot[Anime_.uuid].`in`(sortedPagedIds))

            val entitiesMap = createReadOnlyQuery(entityManager, entityQuery).resultList.associateBy { it.uuid }
            val sortedEntities = sortedPagedIds.mapNotNull { entitiesMap[it] }

            Pageable(
                sortedEntities.toSet(),
                page,
                limit,
                filteredIds.size.toLong()
            )
        }
    }

    fun findAllByFirstLetterCategory(
        countryCode: CountryCode?,
        firstLetter: String,
        page: Int,
        limit: Int,
        searchTypes: Array<LangType>?
    ): Pageable<Anime> {
        return database.entityManager.use {
            val cb = it.criteriaBuilder
            val query = cb.createQuery(getEntityClass())
            val root = query.from(getEntityClass())
            query.distinct(true).select(root)

            val predicates = mutableListOf<Predicate>(
                cb.equal(
                    cb.function(
                        "REGEXP_REPLACE", String::class.java,
                        cb.upper(cb.substring(root[Anime_.slug], 1, 1)),
                        cb.literal("[^A-Z]"),
                        cb.literal("#"),
                    ),
                    firstLetter
                )
            )

            val orPredicate = predicates(countryCode, predicates, cb, root, searchTypes)

            query.where(
                *predicates.toTypedArray(),
                if (orPredicate.isNotEmpty()) cb.or(*orPredicate.toTypedArray()) else cb.conjunction()
            )

            query.orderBy(cb.asc(root[Anime_.slug]))

            buildPageableQuery(createReadOnlyQuery(it, query), page, limit)
        }
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