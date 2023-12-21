package fr.shikkanime.repositories

import fr.shikkanime.entities.Anime
import fr.shikkanime.entities.SortParameter
import fr.shikkanime.entities.enums.CountryCode
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
            it.createQuery("FROM Anime", getEntityClass())
                .resultList
                .initialize()
        }
    }

    fun findAll(sort: List<SortParameter>, page: Int, limit: Int): List<Anime> {
        return inTransaction {
            val query = StringBuilder("FROM Anime a")
            buildSortQuery(sort, query)

            it.createQuery(query.toString(), getEntityClass())
                .setFirstResult(limit * page - limit)
                .setMaxResults(limit)
                .resultList
                .initialize()
        }
    }

    fun findByLikeName(countryCode: CountryCode, name: String?): List<Anime> {
        return inTransaction {
            it.createQuery("FROM Anime WHERE countryCode = :countryCode AND LOWER(name) LIKE :name", getEntityClass())
                .setParameter("countryCode", countryCode)
                .setParameter("name", "%${name?.lowercase()}%")
                .resultList
                .initialize()
        }
    }

    fun findByName(name: String?, countryCode: CountryCode, page: Int, limit: Int): List<Anime> {
        return inTransaction {
            val searchSession = Search.session(it)

            val searchResult: SearchResult<Anime> = searchSession.search(getEntityClass())
                .where { w -> findWhere(w, name, countryCode) }
                .fetch((page - 1) * limit, limit) as SearchResult<Anime>

            searchResult.hits().initialize()
        }
    }

    private fun findWhere(
        searchPredicateFactory: SearchPredicateFactory,
        name: String?,
        countryCode: CountryCode
    ): BooleanPredicateClausesStep<*>? {
        val bool = searchPredicateFactory.bool()

        if (name != null) {
            bool.must(searchPredicateFactory.match().field("name").matching(name))
        }

        bool.must(searchPredicateFactory.match().field("countryCode").matching(countryCode))
        return bool
    }

    fun findBySimulcast(uuid: UUID, countryCode: CountryCode, sort: List<SortParameter>, page: Int, limit: Int): List<Anime> {
        return inTransaction {
            val query = StringBuilder("SELECT a FROM Anime a JOIN a.simulcasts s WHERE a.countryCode = :countryCode AND s.uuid = :uuid")
            buildSortQuery(sort, query)

            it.createQuery(query.toString(), getEntityClass())
                .setParameter("countryCode", countryCode)
                .setParameter("uuid", uuid)
                .setFirstResult(limit * page - limit)
                .setMaxResults(limit)
                .resultList
                .initialize()
        }
    }

    override fun find(uuid: UUID): Anime? {
        return inTransaction {
            it.createQuery("FROM Anime WHERE uuid = :uuid", getEntityClass())
                .setParameter("uuid", uuid)
                .resultList
                .firstOrNull()
                ?.initialize()
        }
    }
}