package fr.shikkanime.repositories

import fr.shikkanime.dtos.enums.Status
import fr.shikkanime.entities.*
import fr.shikkanime.entities.enums.CountryCode
import fr.shikkanime.entities.enums.EpisodeType
import fr.shikkanime.entities.enums.Platform
import jakarta.persistence.Tuple
import jakarta.persistence.criteria.Predicate
import java.util.*

class EpisodeMappingRepository : AbstractRepository<EpisodeMapping>() {
    override fun getEntityClass() = EpisodeMapping::class.java

    fun findAllBy(
        countryCode: CountryCode?,
        anime: Anime?,
        sort: List<SortParameter>,
        page: Int,
        limit: Int,
        status: Status? = null
    ): Pageable<EpisodeMapping> {
        return inTransaction { entityManager ->
            val cb = entityManager.criteriaBuilder
            val query = cb.createQuery(getEntityClass())
            val root = query.from(getEntityClass())

            val predicates = mutableListOf<Predicate>()
            anime?.let { predicates.add(cb.equal(root[EpisodeMapping_.anime], it)) }
            countryCode?.let { predicates.add(cb.equal(root[EpisodeMapping_.anime][Anime_.countryCode], it)) }
            status?.let { predicates.add(cb.equal(root[EpisodeMapping_.status], it)) }
            query.where(*predicates.toTypedArray())

            val orders = sort.mapNotNull { sortParameter ->
                val order = if (sortParameter.order == SortParameter.Order.ASC) cb::asc else cb::desc

                val field = when (sortParameter.field) {
                    "episodeType" -> root[EpisodeMapping_.episodeType]
                    "releaseDateTime" -> root[EpisodeMapping_.releaseDateTime]
                    "lastReleaseDateTime" -> root[EpisodeMapping_.lastReleaseDateTime]
                    "season" -> root[EpisodeMapping_.season]
                    "number" -> root[EpisodeMapping_.number]
                    "animeName" -> root[EpisodeMapping_.anime][Anime_.name]
                    else -> null
                }

                field?.let { order(it) }
            }

            query.orderBy(orders)
            buildPageableQuery(entityManager.createQuery(query), page, limit)
        }
    }

    fun findAllUuidAndImage(): List<Tuple> {
        return inTransaction { entityManager ->
            val cb = entityManager.criteriaBuilder
            val query = cb.createTupleQuery()
            val root = query.from(getEntityClass())
            query.multiselect(root.get<UUID>(EpisodeMapping_.UUID), root[EpisodeMapping_.image])
            entityManager.createQuery(query).resultList
        }
    }

    fun findAllByAnime(anime: Anime): List<EpisodeMapping> {
        return inTransaction {
            val cb = it.criteriaBuilder
            val query = cb.createQuery(getEntityClass())
            val root = query.from(getEntityClass())

            query.where(cb.equal(root[EpisodeMapping_.anime], anime))

            it.createQuery(query)
                .resultList
        }
    }

    fun findByAnimeEpisodeTypeSeasonNumber(
        anime: Anime,
        episodeType: EpisodeType,
        season: Int,
        number: Int
    ): EpisodeMapping? {
        return inTransaction {
            val cb = it.criteriaBuilder
            val query = cb.createQuery(getEntityClass())
            val root = query.from(getEntityClass())

            query.where(
                cb.and(
                    cb.equal(root[EpisodeMapping_.anime], anime),
                    cb.equal(root[EpisodeMapping_.episodeType], episodeType),
                    cb.equal(root[EpisodeMapping_.season], season),
                    cb.equal(root[EpisodeMapping_.number], number)
                )
            )

            it.createQuery(query)
                .resultList
                .firstOrNull()
        }
    }

    fun findLastNumber(
        anime: Anime,
        episodeType: EpisodeType,
        season: Int,
        platform: Platform,
        audioLocale: String
    ): Int {
        return inTransaction {
            val cb = it.criteriaBuilder
            val query = cb.createQuery(Int::class.java)
            val root = query.from(EpisodeVariant::class.java)

            query.select(root[EpisodeVariant_.mapping][EpisodeMapping_.number])

            query.where(
                cb.and(
                    cb.equal(root[EpisodeVariant_.mapping][EpisodeMapping_.anime], anime),
                    cb.equal(root[EpisodeVariant_.mapping][EpisodeMapping_.season], season),
                    cb.equal(root[EpisodeVariant_.platform], platform),
                    cb.equal(root[EpisodeVariant_.mapping][EpisodeMapping_.episodeType], episodeType),
                    cb.equal(root[EpisodeVariant_.audioLocale], audioLocale)
                )
            )

            query.orderBy(cb.desc(root[EpisodeVariant_.mapping][EpisodeMapping_.number]))
            it.createQuery(query)
                .resultList
                .firstOrNull() ?: 0
        }
    }
}