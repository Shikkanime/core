package fr.shikkanime.repositories

import fr.shikkanime.dtos.enums.Status
import fr.shikkanime.entities.*
import fr.shikkanime.entities.enums.CountryCode
import fr.shikkanime.entities.enums.EpisodeType
import fr.shikkanime.entities.enums.Platform
import jakarta.persistence.Tuple
import jakarta.persistence.criteria.Predicate
import java.time.ZonedDateTime

class EpisodeMappingRepository : AbstractRepository<EpisodeMapping>() {
    override fun getEntityClass() = EpisodeMapping::class.java

    fun findAllBy(
        countryCode: CountryCode?,
        anime: Anime?,
        season: Int?,
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
            season?.let { predicates.add(cb.equal(root[EpisodeMapping_.season], it)) }
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
            buildPageableQuery(createReadOnlyQuery(entityManager, query), page, limit)
        }
    }

    fun findAllUuidAndImage(): List<Tuple> {
        return inTransaction {
            val cb = it.criteriaBuilder
            val query = cb.createTupleQuery()
            val root = query.from(getEntityClass())
            query.multiselect(root[EpisodeMapping_.uuid], root[EpisodeMapping_.image])
            createReadOnlyQuery(it, query)
                .resultList
        }
    }

    fun findAllByAnime(anime: Anime): List<EpisodeMapping> {
        return inTransaction {
            val cb = it.criteriaBuilder
            val query = cb.createQuery(getEntityClass())
            val root = query.from(getEntityClass())

            query.where(cb.equal(root[EpisodeMapping_.anime], anime))

            createReadOnlyQuery(it, query)
                .resultList
        }
    }

    fun findAllSeasonsByAnime(anime: Anime): List<Tuple> {
        return inTransaction {
            val cb = it.criteriaBuilder
            val query = cb.createTupleQuery()
            val root = query.from(getEntityClass())

            query.multiselect(root[EpisodeMapping_.season], cb.greatest(root[EpisodeMapping_.lastReleaseDateTime]))
            query.groupBy(root[EpisodeMapping_.season])
            query.where(cb.equal(root[EpisodeMapping_.anime], anime))
            query.orderBy(cb.asc(root[EpisodeMapping_.season]))
            query.distinct(true)

            createReadOnlyQuery(it, query)
                .resultList
        }
    }

    fun findAllNeedUpdateByPlatform(platform: Platform, lastDateTime: ZonedDateTime): List<EpisodeMapping> {
        return inTransaction {
            val cb = it.criteriaBuilder
            val query = cb.createQuery(getEntityClass())
            val root = query.from(getEntityClass())

            // Platform is in the episode variant list
            // Subquery to get the episode mapping with the platform
            val subQuery = query.subquery(EpisodeMapping::class.java)
            val subRoot = subQuery.from(EpisodeVariant::class.java)
            subQuery.select(subRoot[EpisodeVariant_.mapping])

            subQuery.where(
                cb.and(
                    cb.equal(subRoot[EpisodeVariant_.platform], platform),
                    cb.equal(subRoot[EpisodeVariant_.mapping], root)
                )
            )

            query.where(
                cb.and(
                    cb.or(
                        cb.lessThanOrEqualTo(root[EpisodeMapping_.lastUpdateDateTime], lastDateTime),
                        cb.equal(root[EpisodeMapping_.status], Status.INVALID),
                    ),
                    cb.exists(subQuery)
                ),
            )

            query.orderBy(cb.asc(root[EpisodeMapping_.lastUpdateDateTime]))

            createReadOnlyQuery(it, query)
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

            createReadOnlyQuery(it, query)
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
            createReadOnlyQuery(it, query)
                .resultList
                .firstOrNull() ?: 0
        }
    }
}