package fr.shikkanime.repositories

import fr.shikkanime.entities.Anime
import fr.shikkanime.entities.Pageable
import fr.shikkanime.entities.SortParameter
import fr.shikkanime.entities.enums.CountryCode
import jakarta.persistence.Tuple
import org.hibernate.Hibernate
import org.hibernate.search.engine.search.predicate.dsl.BooleanPredicateClausesStep
import org.hibernate.search.engine.search.predicate.dsl.SearchPredicateFactory
import org.hibernate.search.engine.search.query.SearchResult
import org.hibernate.search.mapper.orm.Search
import java.util.*

class AnimeRepository : AbstractRepository<Anime>() {
    private fun Anime.initialize(): Anime {
        Hibernate.initialize(this.simulcasts)
        return this
    }

    private fun List<Anime>.initialize(): List<Anime> {
        this.forEach { anime -> anime.initialize() }
        return this
    }

    private fun Pageable<Anime>.initialize(): Pageable<Anime> {
        this.data.forEach { anime -> anime.initialize() }
        return this
    }

    private fun addOrderBy(query: StringBuilder) {
        if (!query.contains("ORDER BY")) {
            query.append(" ORDER BY")
        }
    }

    private fun buildSortQuery(sort: List<SortParameter>, query: StringBuilder) {
        if (sort.any { param -> param.field == "name" }) {
            val param = sort.first { param -> param.field == "name" }
            addOrderBy(query)
            query.append(" LOWER(a.name) ${param.order.name}")
        }

        if (sort.any { param -> param.field == "releaseDateTime" }) {
            val param = sort.first { param -> param.field == "releaseDateTime" }
            addOrderBy(query)
            query.append(" a.releaseDateTime ${param.order.name}")
        }
    }

    fun preIndex() {
        inTransaction {
            val searchSession = Search.session(it)
            val indexer = searchSession.massIndexer(getEntityClass())
            indexer.startAndWait()
        }
    }

    override fun findAll(): List<Anime> {
        return inTransaction {
            createReadOnlyQuery(it, "FROM Anime", getEntityClass())
                .resultList
                .initialize()
        }
    }

    fun findAllBy(
        countryCode: CountryCode?,
        simulcast: UUID?,
        sort: List<SortParameter>,
        page: Int,
        limit: Int
    ): Pageable<Anime> {
        val queryBuilder = StringBuilder("FROM Anime a")
        val whereClause = mutableListOf<String>()

        simulcast?.let {
            queryBuilder.append(" JOIN a.simulcasts s")
            whereClause.add("s.uuid = :uuid")
        }
        countryCode?.let { whereClause.add("a.countryCode = :countryCode") }

        if (whereClause.isNotEmpty()) {
            queryBuilder.append(" WHERE ${whereClause.joinToString(" AND ")}")
        }

        buildSortQuery(sort, queryBuilder)

        return inTransaction {
            val query = createReadOnlyQuery(it, queryBuilder.toString(), getEntityClass())
            countryCode?.let { query.setParameter("countryCode", countryCode) }
            simulcast?.let { query.setParameter("uuid", simulcast) }
            buildPageableQuery(query, page, limit).initialize()
        }
    }

    fun findAllByLikeName(countryCode: CountryCode, name: String?): List<Anime> {
        return inTransaction {
            createReadOnlyQuery(it, "FROM Anime WHERE countryCode = :countryCode AND LOWER(name) LIKE :name", getEntityClass())
                .setParameter("countryCode", countryCode)
                .setParameter("name", "%${name?.lowercase()}%")
                .resultList
                .initialize()
        }
    }

    fun findAllByName(name: String, countryCode: CountryCode?, page: Int, limit: Int): Pageable<Anime> {
        val searchSession = Search.session(database.entityManager)

        @Suppress("UNCHECKED_CAST")
        val searchResult = searchSession.search(Anime::class.java)
            .where { w -> findWhere(w, name, countryCode) }
            .fetch((limit * page) - limit, limit) as SearchResult<Anime>

        return inTransaction {
            Pageable(searchResult.hits(), page, limit, searchResult.total().hitCount()).initialize()
        }
    }

    private fun findWhere(
        searchPredicateFactory: SearchPredicateFactory,
        name: String,
        countryCode: CountryCode?
    ): BooleanPredicateClausesStep<*>? {
        val bool = searchPredicateFactory.bool()
        bool.must(searchPredicateFactory.match().field("name").matching(name))
        countryCode?.let { bool.must(searchPredicateFactory.match().field("countryCode").matching(it)) }
        return bool
    }

    fun findBySlug(slug: String): Anime? {
        return inTransaction {
            createReadOnlyQuery(it, "FROM Anime WHERE slug = :slug", getEntityClass())
                .setParameter("slug", slug)
                .resultList
                .firstOrNull()
                ?.initialize()
        }
    }

    override fun find(uuid: UUID): Anime? {
        return inTransaction {
            createReadOnlyQuery(it, "FROM Anime WHERE uuid = :uuid", getEntityClass())
                .setParameter("uuid", uuid)
                .resultList
                .firstOrNull()
                ?.initialize()
        }
    }

    fun findAllUUIDAndImage(): List<Tuple> {
        return inTransaction {
            createReadOnlyQuery(it, "SELECT uuid, image, banner FROM Anime", Tuple::class.java)
                .resultList
        }
    }
}