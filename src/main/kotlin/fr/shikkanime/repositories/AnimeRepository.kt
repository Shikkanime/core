package fr.shikkanime.repositories

import fr.shikkanime.dtos.enums.Status
import fr.shikkanime.entities.*
import fr.shikkanime.entities.enums.CountryCode
import jakarta.persistence.Tuple
import jakarta.persistence.criteria.Predicate
import org.hibernate.Hibernate
import org.hibernate.search.engine.search.predicate.dsl.BooleanPredicateClausesStep
import org.hibernate.search.engine.search.predicate.dsl.SearchPredicateFactory
import org.hibernate.search.engine.search.query.SearchResult
import org.hibernate.search.mapper.orm.Search

class AnimeRepository : AbstractRepository<Anime>() {
    override fun getEntityClass() = Anime::class.java

    fun preIndex() {
        inTransaction {
            val searchSession = Search.session(it)
            val indexer = searchSession.massIndexer(getEntityClass())
            indexer.startAndWait()
        }
    }

    override fun findAll(): List<Anime> {
        return inTransaction {
            val cb = it.criteriaBuilder
            val query = cb.createQuery(getEntityClass())
            query.from(getEntityClass())

            val list = it.createQuery(query).resultList
            list.forEach { anime -> Hibernate.initialize(anime.mappings) }
            list
        }
    }

    fun findAllBy(
        countryCode: CountryCode?,
        simulcast: Simulcast?,
        sort: List<SortParameter>,
        page: Int,
        limit: Int,
        status: Status? = null
    ): Pageable<Anime> {
        return inTransaction { entityManager ->
            val cb = entityManager.criteriaBuilder
            val query = cb.createQuery(getEntityClass())
            val root = query.from(getEntityClass())

            val predicates = mutableListOf<Predicate>()
            simulcast?.let { predicates.add(cb.equal(root.join(Anime_.simulcasts), it)) }
            countryCode?.let { predicates.add(cb.equal(root[Anime_.countryCode], it)) }
            status?.let { predicates.add(cb.equal(root[Anime_.status], it)) }
            query.where(*predicates.toTypedArray())

            val orders = sort.mapNotNull { sortParameter ->
                val order = if (sortParameter.order == SortParameter.Order.ASC) cb::asc else cb::desc

                val field = when (sortParameter.field) {
                    "name" -> root[Anime_.name]
                    "releaseDateTime" -> root[Anime_.releaseDateTime]
                    "lastReleaseDateTime" -> root[Anime_.lastReleaseDateTime]
                    else -> null
                }

                field?.let { order(it) }
            }

            query.orderBy(orders)
            buildPageableQuery(createReadOnlyQuery(entityManager, query), page, limit)
        }
    }

    fun findAllByName(name: String, countryCode: CountryCode?, page: Int, limit: Int): Pageable<Anime> {
        val searchSession = Search.session(database.entityManager)

        @Suppress("UNCHECKED_CAST")
        val searchResult = searchSession.search(getEntityClass())
            .where { w -> findWhere(w, name, countryCode) }
            .fetch((limit * page) - limit, limit) as SearchResult<Anime>

        return inTransaction {
            Pageable(searchResult.hits(), page, limit, searchResult.total().hitCount())
        }
    }

    private fun findWhere(
        searchPredicateFactory: SearchPredicateFactory,
        name: String,
        countryCode: CountryCode?
    ): BooleanPredicateClausesStep<*>? {
        val bool = searchPredicateFactory.bool()
        bool.must(searchPredicateFactory.match().field(Anime_.NAME).matching(name))
        countryCode?.let { bool.must(searchPredicateFactory.match().field(Anime_.COUNTRY_CODE).matching(it)) }
        return bool
    }

    fun findAllUuidImageAndBanner(): List<Tuple> {
        return inTransaction { entityManager ->
            val cb = entityManager.criteriaBuilder
            val query = cb.createTupleQuery()
            val root = query.from(getEntityClass())
            query.multiselect(root[Anime_.uuid], root[Anime_.image], root[Anime_.banner])
            entityManager.createQuery(query).resultList
        }
    }

    fun findBySlug(countryCode: CountryCode, slug: String): Anime? {
        return inTransaction { entityManager ->
            val cb = entityManager.criteriaBuilder
            val query = cb.createQuery(getEntityClass())
            val root = query.from(getEntityClass())

            query.where(
                cb.and(
                    cb.equal(root[Anime_.countryCode], countryCode),
                    cb.equal(root[Anime_.slug], slug)
                )
            )

            createReadOnlyQuery(entityManager, query)
                .resultList
                .firstOrNull()
        }
    }

    fun findByName(countryCode: CountryCode, name: String?): Anime? {
        return inTransaction {
            val cb = it.criteriaBuilder
            val query = cb.createQuery(getEntityClass())
            val root = query.from(getEntityClass())

            query.where(
                cb.and(
                    cb.equal(root[Anime_.countryCode], countryCode),
                    cb.equal(cb.lower(root[Anime_.name]), name?.lowercase())
                )
            )

            it.createQuery(query)
                .resultList
                .firstOrNull()
        }
    }
}