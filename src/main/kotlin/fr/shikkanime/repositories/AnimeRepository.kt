package fr.shikkanime.repositories

import fr.shikkanime.dtos.enums.Status
import fr.shikkanime.entities.*
import fr.shikkanime.entities.enums.CountryCode
import fr.shikkanime.entities.enums.LangType
import jakarta.persistence.Tuple
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
        status: Status? = null
    ): Pageable<Anime> {
        return database.entityManager.use { entityManager ->
            val cb = entityManager.criteriaBuilder
            val query = cb.createQuery(getEntityClass())
            val root = query.from(getEntityClass())
            query.distinct(true).select(root)

            val predicates = mutableListOf<Predicate>()
            simulcast?.let { predicates.add(cb.equal(root.join(Anime_.simulcasts), it)) }
            status?.let { predicates.add(cb.equal(root[Anime_.status], it)) }
            val orPredicate = predicates(countryCode, predicates, cb, root, searchTypes)

            query.where(
                *predicates.toTypedArray(),
                if (orPredicate.isNotEmpty()) cb.or(*orPredicate.toTypedArray()) else cb.conjunction()
            )

            val orders = sort.mapNotNull { sortParameter ->
                val field = when (sortParameter.field) {
                    "name" -> root[Anime_.name]
                    "releaseDateTime" -> root[Anime_.releaseDateTime]
                    "lastReleaseDateTime" -> root[Anime_.lastReleaseDateTime]
                    else -> null
                }

                field?.let { (if (sortParameter.order == SortParameter.Order.ASC) cb::asc else cb::desc).invoke(it) }
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
        return database.entityManager.use {
            val ids = (Search.session(it).search(getEntityClass())
                // Select id and score
                .select { s -> s.composite(s.id(), s.score()) }
                .where { w -> w.bool().must(w.match().field(Anime_.NAME).matching(name)) }
                .fetchAll().hits().filterIsInstance<List<Any>>())
                .map { array -> Pair(array[0] as UUID, array[1] as Float) }

            val cb = it.criteriaBuilder
            val query = cb.createQuery(getEntityClass())
            val root = query.from(getEntityClass())
            val predicates = mutableListOf<Predicate>(root[Anime_.uuid].`in`(ids.map { pair -> pair.first }))
            val orPredicate = predicates(countryCode, predicates, cb, root, searchTypes)

            query.where(
                *predicates.toTypedArray(),
                if (orPredicate.isNotEmpty()) cb.or(*orPredicate.toTypedArray()) else cb.conjunction()
            )

            val list = createReadOnlyQuery(it, query)
                .resultList
                .sortedByDescending { anime -> ids.first { pair -> pair.first == anime.uuid }.second }

            Pageable(
                list.asSequence().drop((page - 1) * limit).take(limit).toSet(),
                page,
                limit,
                list.size.toLong()
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
                    cb.upper(
                        cb.function(
                            "REGEXP_REPLACE", String::class.java,
                            cb.upper(cb.substring(root[Anime_.name], 1, 1)),
                            cb.literal("[^A-Z]"),
                            cb.literal("#"),
                        )
                    ),
                    firstLetter
                )
            )
            val orPredicate = predicates(countryCode, predicates, cb, root, searchTypes)

            query.where(
                *predicates.toTypedArray(),
                if (orPredicate.isNotEmpty()) cb.or(*orPredicate.toTypedArray()) else cb.conjunction()
            )

            query.orderBy(cb.asc(root[Anime_.name]))

            buildPageableQuery(createReadOnlyQuery(it, query), page, limit)
        }
    }

    fun findAllUuidAndSlug(): List<Tuple> {
        return database.entityManager.use {
            val cb = it.criteriaBuilder
            val query = cb.createTupleQuery()
            val root = query.from(getEntityClass())
            query.multiselect(root[Anime_.uuid], root[Anime_.slug])

            createReadOnlyQuery(it, query)
                .resultList
        }
    }

    fun findAllNeedUpdate(lastDateTime: ZonedDateTime): List<Anime> {
        return database.entityManager.use {
            val cb = it.criteriaBuilder
            val query = cb.createQuery(getEntityClass())
            val root = query.from(getEntityClass())

            query.where(
                cb.or(
                    cb.isNull(root[Anime_.lastUpdateDateTime]),
                    cb.lessThanOrEqualTo(root[Anime_.lastUpdateDateTime], lastDateTime),
                ),
                cb.isNotEmpty(root[Anime_.platformIds]),
            ).orderBy(cb.asc(root[Anime_.lastUpdateDateTime]))

            createReadOnlyQuery(it, query)
                .resultList
        }
    }

    fun findAllAudioLocales(): Map<UUID, Set<String>> {
        return database.entityManager.use {
            val cb = it.criteriaBuilder
            val query = cb.createTupleQuery()
            val root = query.from(getEntityClass())

            query.distinct(true)
                .multiselect(
                    root[Anime_.uuid],
                    root.join(Anime_.mappings).join(EpisodeMapping_.variants)[EpisodeVariant_.audioLocale]
                )

            createReadOnlyQuery(it, query)
                .resultList
                .asSequence()
                .groupBy({ tuple -> tuple[0] as UUID }, { tuple -> tuple[1] as String })
                .mapValues { (_, locales) -> locales.toSet() }
        }
    }

    fun findAllSeasons(): Map<UUID, Map<Int, ZonedDateTime>> {
        return database.entityManager.use {
            val cb = it.criteriaBuilder
            val query = cb.createTupleQuery()
            val root = query.from(getEntityClass())
            val mappingJoin = root.join(Anime_.mappings)

            query.multiselect(
                root[Anime_.uuid],
                mappingJoin[EpisodeMapping_.season],
                cb.greatest(mappingJoin[EpisodeMapping_.lastReleaseDateTime])
            ).groupBy(root[Anime_.uuid], mappingJoin[EpisodeMapping_.season])
                .orderBy(cb.asc(mappingJoin[EpisodeMapping_.season]))

            createReadOnlyQuery(it, query)
                .resultList
                .asSequence()
                .groupBy({ tuple -> tuple[0] as UUID }, { tuple -> tuple[1] as Int to tuple[2] as ZonedDateTime })
                .mapValues { (_, seasons) -> seasons.toMap() }
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
        inTransaction {
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