package fr.shikkanime.repositories

import fr.shikkanime.dtos.enums.Status
import fr.shikkanime.entities.*
import fr.shikkanime.entities.enums.CountryCode
import fr.shikkanime.entities.enums.EpisodeType
import fr.shikkanime.entities.enums.Platform
import fr.shikkanime.utils.Constant
import jakarta.persistence.Tuple
import jakarta.persistence.criteria.Predicate
import java.time.ZonedDateTime
import java.util.*

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
        return database.entityManager.use { entityManager ->
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
        return database.entityManager.use {
            val cb = it.criteriaBuilder
            val query = cb.createTupleQuery()
            val root = query.from(getEntityClass())
            query.multiselect(root[EpisodeMapping_.uuid], root[EpisodeMapping_.image])

            createReadOnlyQuery(it, query)
                .resultList
        }
    }

    fun findAllByAnime(anime: Anime): List<EpisodeMapping> {
        return database.entityManager.use {
            val cb = it.criteriaBuilder
            val query = cb.createQuery(getEntityClass())
            val root = query.from(getEntityClass())

            query.where(cb.equal(root[EpisodeMapping_.anime], anime))

            createReadOnlyQuery(it, query)
                .resultList
        }
    }

    fun findAllNeedUpdateByPlatform(platform: Platform, lastDateTime: ZonedDateTime): List<EpisodeMapping> {
        return database.entityManager.use {
            val cb = it.criteriaBuilder
            val query = cb.createQuery(getEntityClass())
            val root = query.from(EpisodeVariant::class.java)

            query.distinct(true)
                .select(root[EpisodeVariant_.mapping])
                .where(
                    cb.equal(root[EpisodeVariant_.platform], platform),
                    cb.or(
                        cb.lessThanOrEqualTo(
                            root[EpisodeVariant_.mapping][EpisodeMapping_.lastUpdateDateTime],
                            lastDateTime
                        ),
                        cb.equal(root[EpisodeVariant_.mapping][EpisodeMapping_.image], Constant.DEFAULT_IMAGE_PREVIEW),
                    ),
                )
                .orderBy(cb.asc(root[EpisodeVariant_.mapping][EpisodeMapping_.lastUpdateDateTime]))

            createReadOnlyQuery(it, query)
                .resultList
        }
    }

    fun findAllSeo(): List<Tuple> {
        return database.entityManager.use {
            val cb = it.criteriaBuilder
            val query = cb.createTupleQuery()
            val root = query.from(getEntityClass())

            query.multiselect(
                root[EpisodeMapping_.anime][Anime_.slug],
                root[EpisodeMapping_.season],
                root[EpisodeMapping_.episodeType],
                root[EpisodeMapping_.number],
                root[EpisodeMapping_.lastReleaseDateTime],
            )

            query.orderBy(cb.asc(root[EpisodeMapping_.releaseDateTime]))

            createReadOnlyQuery(it, query)
                .resultList
        }
    }

    fun findByAnimeSeasonEpisodeTypeNumber(
        animeUuid: UUID,
        season: Int,
        episodeType: EpisodeType,
        number: Int
    ): EpisodeMapping? {
        return database.entityManager.use {
            val cb = it.criteriaBuilder
            val query = cb.createQuery(getEntityClass())
            val root = query.from(getEntityClass())

            query.where(
                cb.and(
                    cb.equal(root[EpisodeMapping_.anime][Anime_.uuid], animeUuid),
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
        return database.entityManager.use {
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

    fun findPreviousEpisode(
        episodeMapping: EpisodeMapping,
    ): EpisodeMapping? {
        return database.entityManager.use {
            // Sort on release date time to get the previous episode
            // If the release date time is the same, sort on the number
            val cb = it.criteriaBuilder
            val query = cb.createQuery(getEntityClass())
            val root = query.from(getEntityClass())

            query.where(
                cb.and(
                    cb.equal(root[EpisodeMapping_.anime], episodeMapping.anime),
                    cb.equal(root[EpisodeMapping_.season], episodeMapping.season),
                    cb.or(
                        cb.lessThan(root[EpisodeMapping_.releaseDateTime], episodeMapping.releaseDateTime),
                        cb.and(
                            cb.equal(root[EpisodeMapping_.episodeType], episodeMapping.episodeType),
                            cb.lessThan(root[EpisodeMapping_.number], episodeMapping.number!!),
                        )
                    ),
                    cb.notEqual(root[EpisodeMapping_.uuid], episodeMapping.uuid)
                )
            )

            query.orderBy(
                cb.desc(root[EpisodeMapping_.releaseDateTime]),
                cb.desc(root[EpisodeMapping_.number])
            )

            createReadOnlyQuery(it, query)
                .resultList
                .firstOrNull()
        }
    }

    fun findNextEpisode(
        episodeMapping: EpisodeMapping,
    ): EpisodeMapping? {
        return database.entityManager.use {
            // Sort on release date time to get the next episode
            // If the release date time is the same, sort on the number
            val cb = it.criteriaBuilder
            val query = cb.createQuery(getEntityClass())
            val root = query.from(getEntityClass())

            query.where(
                cb.and(
                    cb.equal(root[EpisodeMapping_.anime], episodeMapping.anime),
                    cb.equal(root[EpisodeMapping_.season], episodeMapping.season),
                    cb.or(
                        cb.greaterThan(root[EpisodeMapping_.releaseDateTime], episodeMapping.releaseDateTime),
                        cb.and(
                            cb.equal(root[EpisodeMapping_.episodeType], episodeMapping.episodeType),
                            cb.greaterThan(root[EpisodeMapping_.number], episodeMapping.number!!),
                        ),
                    ),
                    cb.notEqual(root[EpisodeMapping_.uuid], episodeMapping.uuid)
                )
            )

            query.orderBy(
                cb.asc(root[EpisodeMapping_.releaseDateTime]),
                cb.asc(root[EpisodeMapping_.number])
            )

            createReadOnlyQuery(it, query)
                .resultList
                .firstOrNull()
        }
    }

    fun findPreviousReleaseDateOfSimulcastedEpisodeMapping(
        anime: Anime,
        episodeMapping: EpisodeMapping
    ): ZonedDateTime? {
        return database.entityManager.use {
            val cb = it.criteriaBuilder
            val query = cb.createQuery(ZonedDateTime::class.java)
            val root = query.from(EpisodeVariant::class.java)
            query.select(root[EpisodeVariant_.mapping][EpisodeMapping_.releaseDateTime])

            query.where(
                cb.and(
                    cb.equal(root[EpisodeVariant_.mapping][EpisodeMapping_.anime], anime),
                    cb.lessThan(
                        root[EpisodeVariant_.mapping][EpisodeMapping_.releaseDateTime],
                        episodeMapping.releaseDateTime
                    ),
                    cb.equal(root[EpisodeVariant_.mapping][EpisodeMapping_.episodeType], episodeMapping.episodeType),
                    cb.notEqual(root[EpisodeVariant_.audioLocale], anime.countryCode!!.locale),
                )
            )

            query.orderBy(cb.desc(root[EpisodeVariant_.mapping][EpisodeMapping_.releaseDateTime]))

            createReadOnlyQuery(it, query)
                .setMaxResults(1)
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
            val subRootMin = subQueryMin.from(EpisodeVariant::class.java)
            subQueryMin.select(cb.least(subRootMin[EpisodeVariant_.releaseDateTime]))
            subQueryMin.where(cb.equal(subRootMin[EpisodeVariant_.mapping], root))

            val subQueryMax = update.subquery(ZonedDateTime::class.java)
            val subRootMax = subQueryMax.from(EpisodeVariant::class.java)
            subQueryMax.select(cb.greatest(subRootMax[EpisodeVariant_.releaseDateTime]))
            subQueryMax.where(cb.equal(subRootMax[EpisodeVariant_.mapping], root))

            update[root[EpisodeMapping_.releaseDateTime]] = subQueryMin
            update[root[EpisodeMapping_.lastReleaseDateTime]] = subQueryMax

            it.createQuery(update).executeUpdate()
        }
    }
}