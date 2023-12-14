package fr.shikkanime.repositories

import fr.shikkanime.entities.Anime
import fr.shikkanime.entities.enums.CountryCode
import org.hibernate.Hibernate
import org.hibernate.search.engine.search.query.SearchResult
import org.hibernate.search.mapper.orm.Search

class AnimeRepository : AbstractRepository<Anime>() {
    fun preIndex() {
        inTransaction {
            val searchSession = Search.session(it)
            val indexer = searchSession.massIndexer(getEntityClass())
            indexer.startAndWait()
        }
    }

    private fun Anime.initialize(): Anime {
        Hibernate.initialize(this.simulcasts)
        return this
    }

    private fun List<Anime>.initialize(): List<Anime> {
        this.forEach { anime -> anime.initialize() }
        return this
    }

    override fun findAll(): List<Anime> {
        return inTransaction {
            it.createQuery("FROM Anime", getEntityClass())
                .resultList
                .initialize()
        }
    }

    fun findByLikeName(countryCode: CountryCode, name: String?): List<Anime> {
        return inTransaction {
            it.createQuery("FROM Anime where countryCode = :countryCode AND LOWER(name) LIKE :name", getEntityClass())
                .setParameter("countryCode", countryCode)
                .setParameter("name", "%${name?.lowercase()}%")
                .resultList
                .initialize()
        }
    }

    fun findByName(countryCode: CountryCode, name: String?): List<Anime> {
        return inTransaction {
            val searchSession = Search.session(it)

            val searchResult: SearchResult<Anime> = searchSession.search(getEntityClass())
                .where { f ->
                    f.bool()
                        .must(f.match().field("countryCode").matching(countryCode))
                        .must(f.match().field("name").matching(name))
                }
                .fetch(20) as SearchResult<Anime>

            searchResult.hits().initialize()
        }
    }
}