package fr.shikkanime.repositories

import fr.shikkanime.entities.Episode
import fr.shikkanime.entities.Pageable
import fr.shikkanime.entities.SortParameter
import fr.shikkanime.entities.enums.CountryCode
import fr.shikkanime.entities.enums.EpisodeType
import fr.shikkanime.entities.enums.LangType
import fr.shikkanime.entities.enums.Platform
import fr.shikkanime.utils.Constant
import jakarta.persistence.Tuple
import org.hibernate.Hibernate
import java.time.ZonedDateTime
import java.util.*

class EpisodeRepository : AbstractRepository<Episode>() {
    override fun getEntityClass() = Episode::class.java

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
        val fields = listOf("episodeType", "langType", "releaseDateTime", "season", "number", "lastUpdateDateTime")
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
            createReadOnlyQuery(it, "FROM Episode", getEntityClass())
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
        val queryBuilder = StringBuilder("FROM Episode e")
        val whereClause = mutableListOf<String>()

        anime?.let { whereClause.add("e.anime.uuid = :uuid") }
        countryCode?.let { whereClause.add("e.anime.countryCode = :countryCode") }

        if (whereClause.isNotEmpty()) {
            queryBuilder.append(" WHERE ${whereClause.joinToString(" AND ")}")
        }

        buildSortQuery(sort, queryBuilder)

        return inTransaction {
            val query = createReadOnlyQuery(it, queryBuilder.toString(), getEntityClass())
            countryCode?.let { query.setParameter("countryCode", countryCode) }
            anime?.let { query.setParameter("uuid", anime) }
            buildPageableQuery(query, page, limit).initialize()
        }
    }

    fun findAllHashes(): List<String> {
        return inTransaction {
            createReadOnlyQuery(it, "SELECT hash FROM Episode", String::class.java)
                .resultList
        }
    }

    fun findAllByAnime(uuid: UUID): List<Episode> {
        return inTransaction {
            createReadOnlyQuery(it, "FROM Episode WHERE anime.uuid = :uuid", getEntityClass())
                .setParameter("uuid", uuid)
                .resultList
        }
    }

    fun findByHash(hash: String?): Episode? {
        return inTransaction {
            createReadOnlyQuery(it, "FROM Episode WHERE LOWER(hash) LIKE :hash", getEntityClass())
                .setParameter("hash", "%${hash?.lowercase()}%")
                .resultList
                .firstOrNull()
        }
    }

    fun getLastNumber(anime: UUID, platform: Platform, season: Int, episodeType: EpisodeType, langType: LangType): Int {
        return inTransaction {
            val query = createReadOnlyQuery(
                it,
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

    fun findAllUUIDAndImage(): List<Tuple> {
        return inTransaction {
            createReadOnlyQuery(it, "SELECT uuid, image FROM Episode", Tuple::class.java)
                .resultList
        }
    }

    fun findAllByPlatformDeprecatedEpisodes(platform: Platform, lastUpdateDateTime: ZonedDateTime): List<Episode> {
        return inTransaction {
            createReadOnlyQuery(
                it,
                "FROM Episode WHERE platform = :platform AND ((lastUpdateDateTime < :lastUpdateDateTime OR lastUpdateDateTime IS NULL) OR description IS NULL OR image = :defaultImage)",
                getEntityClass()
            )
                .setParameter("platform", platform)
                .setParameter("lastUpdateDateTime", lastUpdateDateTime)
                .setParameter("defaultImage", Constant.DEFAULT_IMAGE_PREVIEW)
                .resultList
                .initialize()
        }
    }
}