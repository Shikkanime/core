package fr.shikkanime.repositories

import fr.shikkanime.entities.Episode
import fr.shikkanime.entities.Pageable
import fr.shikkanime.entities.SortParameter
import fr.shikkanime.entities.enums.CountryCode
import fr.shikkanime.entities.enums.EpisodeType
import fr.shikkanime.entities.enums.LangType
import fr.shikkanime.entities.enums.Platform
import org.hibernate.Hibernate
import java.util.*

class EpisodeRepository : AbstractRepository<Episode>() {
    private fun Episode.initialize(): Episode {
        Hibernate.initialize(this.anime?.simulcasts)
        return this
    }

    private fun List<Episode>.initialize(): List<Episode> {
        this.forEach { episode -> episode.initialize() }
        return this
    }

    private fun Pageable<Episode>.initialize(): Pageable<Episode> {
        this.data.forEach { episode -> episode.initialize() }
        return this
    }

    private fun addOrderBy(query: StringBuilder) {
        if (!query.contains("ORDER BY")) {
            query.append(" ORDER BY")
        }
    }

    private fun buildSortQuery(sort: List<SortParameter>, query: StringBuilder) {
        val fields = listOf("episodeType", "langType", "releaseDateTime", "season", "number")
        val subQuery = mutableListOf<String>()

        sort.filter { fields.contains(it.field) }.forEach { param ->
            val field = param.field
            addOrderBy(query)
            subQuery.add(" e.$field ${param.order.name}")
        }

        if (subQuery.isNotEmpty()) {
            query.append(subQuery.joinToString(", "))
        }
    }

    override fun findAll(): List<Episode> {
        return inTransaction {
            it.createQuery("FROM Episode", getEntityClass())
                .resultList
                .initialize()
        }
    }

    fun findAllBy(
        countryCode: CountryCode?,
        anime: UUID?,
        sort: List<SortParameter>,
        page: Int,
        limit: Int
    ): Pageable<Episode> {
        return inTransaction {
            val queryBuilder = StringBuilder("FROM Episode e")
            val whereClause = mutableListOf<String>()

            anime?.let { whereClause.add("e.anime.uuid = :uuid") }
            countryCode?.let { whereClause.add("e.anime.countryCode = :countryCode") }

            if (whereClause.isNotEmpty()) {
                queryBuilder.append(" WHERE ${whereClause.joinToString(" AND ")}")
            }

            buildSortQuery(sort, queryBuilder)

            val query = it.createQuery(queryBuilder.toString(), getEntityClass())
            countryCode?.let { query.setParameter("countryCode", countryCode) }
            anime?.let { query.setParameter("uuid", anime) }
            buildPageableQuery(query, page, limit).initialize()
        }
    }

    fun findAllHashes(): List<String> {
        return inTransaction {
            it.createQuery("SELECT hash FROM Episode", String::class.java)
                .resultList
        }
    }

    fun findAllByAnime(uuid: UUID): List<Episode> {
        return inTransaction {
            it.createQuery("FROM Episode WHERE anime.uuid = :uuid", getEntityClass())
                .setParameter("uuid", uuid)
                .resultList
        }
    }

    fun findByHash(hash: String?): Episode? {
        return inTransaction {
            it.createQuery("FROM Episode WHERE LOWER(hash) LIKE :hash", getEntityClass())
                .setParameter("hash", "%${hash?.lowercase()}%")
                .resultList
                .firstOrNull()
        }
    }

    fun getLastNumber(anime: UUID, platform: Platform, season: Int, episodeType: EpisodeType, langType: LangType): Int {
        return inTransaction {
            val query = it.createQuery(
                "SELECT number FROM Episode WHERE anime.uuid = :uuid AND platform = :platform AND season = :season AND episodeType = :episodeType AND langType = :langType ORDER BY number DESC",
                Int::class.java
            )
            query.maxResults = 1
            query.setParameter("uuid", anime)
            query.setParameter("platform", platform)
            query.setParameter("season", season)
            query.setParameter("episodeType", episodeType)
            query.setParameter("langType", langType)
            query.resultList.firstOrNull() ?: 0
        }
    }
}