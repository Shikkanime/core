package fr.shikkanime.repositories

import fr.shikkanime.dtos.mappings.EpisodeMappingSeoDto
import fr.shikkanime.entities.*
import fr.shikkanime.entities.enums.CountryCode
import fr.shikkanime.entities.enums.EpisodeType
import fr.shikkanime.entities.enums.ImageType
import fr.shikkanime.entities.enums.Platform
import fr.shikkanime.entities.miscellaneous.Pageable
import fr.shikkanime.entities.miscellaneous.SortParameter
import fr.shikkanime.utils.Constant
import jakarta.persistence.criteria.JoinType
import java.time.ZonedDateTime
import java.util.*

class EpisodeMappingRepository : AbstractRepository<EpisodeMapping>() {
    override fun getEntityClass() = EpisodeMapping::class.java

    fun findAllBy(
        countryCode: CountryCode?,
        animeUuid: UUID?,
        season: Int?,
        sort: List<SortParameter>,
        page: Int,
        limit: Int,
    ): Pageable<EpisodeMapping> {
        return database.entityManager.use { entityManager ->
            val cb = entityManager.criteriaBuilder
            val query = cb.createQuery(getEntityClass())
            val root = query.from(getEntityClass())

            // Build predicates
            val predicates = listOfNotNull(
                animeUuid?.let { cb.equal(root[EpisodeMapping_.anime][Anime_.uuid], it) },
                season?.let { cb.equal(root[EpisodeMapping_.season], it) },
                countryCode?.let { cb.equal(root[EpisodeMapping_.anime][Anime_.countryCode], it) }
            )
            query.where(*predicates.toTypedArray())

            // Build orders
            val orders = sort.mapNotNull { sortParameter ->
                val field = when (sortParameter.field) {
                    "episodeType" -> root[EpisodeMapping_.episodeType]
                    "releaseDateTime" -> root[EpisodeMapping_.releaseDateTime]
                    "lastReleaseDateTime" -> root[EpisodeMapping_.lastReleaseDateTime]
                    "season" -> root[EpisodeMapping_.season]
                    "number" -> root[EpisodeMapping_.number]
                    "animeName" -> root[EpisodeMapping_.anime][Anime_.name]
                    else -> null
                }
                field?.let { if (sortParameter.order == SortParameter.Order.ASC) cb.asc(it) else cb.desc(it) }
            }
            query.orderBy(orders)

            buildPageableQuery(createReadOnlyQuery(entityManager, query), page, limit)
        }
    }

    fun findAllByAnime(animeUuid: UUID): List<EpisodeMapping> {
        return database.entityManager.use {
            val cb = it.criteriaBuilder
            val query = cb.createQuery(getEntityClass())
            val root = query.from(getEntityClass())

            query.where(cb.equal(root[EpisodeMapping_.anime][Anime_.uuid], animeUuid))
                .orderBy(
                    cb.asc(root[EpisodeMapping_.releaseDateTime]),
                    cb.asc(root[EpisodeMapping_.season]),
                    cb.asc(root[EpisodeMapping_.episodeType]),
                    cb.asc(root[EpisodeMapping_.number])
                )

            createReadOnlyQuery(it, query)
                .resultList
        }
    }

    fun findAllNeedUpdate(
        currentSimulcastUuid: UUID?,
        lastSimulcastUuid: UUID?,
        currentSeasonDelay: Long,
        lastSeasonDelay: Long,
        othersDelay: Long,
        lastImageUpdateDelay: Long,
    ): List<EpisodeMapping> {
        return database.entityManager.use {
            val cb = it.criteriaBuilder
            val query = cb.createQuery(getEntityClass())
            val root = query.from(getEntityClass())
            val animeJoin = root.join(EpisodeMapping_.anime)
            val simulcastJoin = animeJoin.join(Anime_.simulcasts, JoinType.LEFT)
            val now = ZonedDateTime.now()

            val predicates = buildSet {
                currentSimulcastUuid?.let { uuid ->
                    add(cb.and(
                        cb.equal(simulcastJoin[Simulcast_.uuid], uuid),
                        cb.lessThanOrEqualTo(root[EpisodeMapping_.lastUpdateDateTime], now.minusDays(currentSeasonDelay))
                    ))
                }
                lastSimulcastUuid?.let { uuid ->
                    add(cb.and(
                        cb.equal(simulcastJoin[Simulcast_.uuid], uuid),
                        cb.lessThanOrEqualTo(root[EpisodeMapping_.lastUpdateDateTime], now.minusDays(lastSeasonDelay))
                    ))
                }
                add(cb.and(
                    cb.not(cb.exists(query.subquery(Int::class.java).apply {
                        val subRoot = from(Anime::class.java)
                        select(cb.literal(1))
                        where(
                            cb.equal(subRoot[Anime_.uuid], animeJoin[Anime_.uuid]),
                            subRoot.join(Anime_.simulcasts)[Simulcast_.uuid]
                                .`in`(listOfNotNull(currentSimulcastUuid, lastSimulcastUuid))
                        )
                    })),
                    cb.lessThanOrEqualTo(root[EpisodeMapping_.lastUpdateDateTime], now.minusDays(othersDelay))
                ))
            }

            val attachmentSubquery = query.subquery(Long::class.java)
            val attachmentRoot = attachmentSubquery.from(Attachment::class.java)

            attachmentSubquery.select(cb.literal(1L))
                .where(
                    cb.equal(attachmentRoot[Attachment_.entityUuid], root[EpisodeMapping_.uuid]),
                    cb.equal(attachmentRoot[Attachment_.type], ImageType.BANNER),
                    cb.equal(attachmentRoot[Attachment_.url], Constant.DEFAULT_IMAGE_PREVIEW),
                    cb.equal(attachmentRoot[Attachment_.active], true)
                )

            query.distinct(true)
                .where(
                    cb.or(
                        *predicates.toTypedArray(),
                        cb.and(
                            cb.equal(attachmentSubquery, 1L),
                            cb.greaterThanOrEqualTo(root[EpisodeMapping_.releaseDateTime], now.minusDays(lastImageUpdateDelay)),
                        ),
                    ),
                )
                .orderBy(cb.asc(root[EpisodeMapping_.lastUpdateDateTime]))

            createReadOnlyQuery(it, query)
                .resultList
        }
    }

    fun findAllSeo(): List<EpisodeMappingSeoDto> {
        return database.entityManager.use {
            val cb = it.criteriaBuilder
            val query = cb.createQuery(EpisodeMappingSeoDto::class.java)
            val root = query.from(getEntityClass())

            query.select(
                cb.construct(
                    EpisodeMappingSeoDto::class.java,
                    root[EpisodeMapping_.anime][Anime_.slug],
                    root[EpisodeMapping_.season],
                    root[EpisodeMapping_.episodeType],
                    root[EpisodeMapping_.number],
                    root[EpisodeMapping_.lastReleaseDateTime]
                )
            )

            createReadOnlyQuery(it, query)
                .resultList
        }
    }

    fun findAllSimulcasted(ignoreEpisodeTypes: Set<EpisodeType>, ignoreAudioLocale: String): List<EpisodeMapping> {
        return database.entityManager.use {
            val cb = it.criteriaBuilder
            val query = cb.createQuery(getEntityClass())
            val root = query.from(EpisodeVariant::class.java)
            query.select(root[EpisodeVariant_.mapping])

            query.where(
                cb.not(root[EpisodeVariant_.mapping][EpisodeMapping_.episodeType].`in`(ignoreEpisodeTypes)),
                cb.notEqual(root[EpisodeVariant_.audioLocale], ignoreAudioLocale)
            ).orderBy(
                cb.asc(root[EpisodeVariant_.mapping][EpisodeMapping_.releaseDateTime]),
                cb.asc(root[EpisodeVariant_.mapping][EpisodeMapping_.season]),
                cb.asc(root[EpisodeVariant_.mapping][EpisodeMapping_.episodeType]),
                cb.asc(root[EpisodeVariant_.mapping][EpisodeMapping_.number])
            ).groupBy(root[EpisodeVariant_.mapping])

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
            val root = query.from(getEntityClass())
            val variantJoin = root.join(EpisodeMapping_.variants)

            query.select(root[EpisodeMapping_.number])

            query.where(
                cb.and(
                    cb.equal(root[EpisodeMapping_.anime], anime),
                    cb.equal(root[EpisodeMapping_.season], season),
                    cb.equal(variantJoin[EpisodeVariant_.platform], platform),
                    cb.equal(root[EpisodeMapping_.episodeType], episodeType),
                    cb.equal(variantJoin[EpisodeVariant_.audioLocale], audioLocale)
                )
            )

            query.orderBy(cb.desc(root[EpisodeMapping_.number]))

            createReadOnlyQuery(it, query)
                .resultList
                .firstOrNull() ?: 0
        }
    }

    fun findPreviousReleaseDateOfSimulcastedEpisodeMapping(
        anime: Anime,
        episodeMapping: EpisodeMapping
    ): ZonedDateTime? {
        return database.entityManager.use {
            val cb = it.criteriaBuilder
            val query = cb.createQuery(ZonedDateTime::class.java)
            val root = query.from(getEntityClass())
            val variantJoin = root.join(EpisodeMapping_.variants)
            query.select(root[EpisodeMapping_.releaseDateTime])

            query.where(
                cb.and(
                    cb.equal(root[EpisodeMapping_.anime], anime),
                    cb.lessThan(
                        root[EpisodeMapping_.releaseDateTime],
                        episodeMapping.releaseDateTime
                    ),
                    cb.equal(root[EpisodeMapping_.episodeType], episodeMapping.episodeType),
                    cb.notEqual(variantJoin[EpisodeVariant_.audioLocale], anime.countryCode!!.locale),
                )
            )

            query.orderBy(cb.desc(root[EpisodeMapping_.releaseDateTime]))

            createReadOnlyQuery(it, query)
                .setMaxResults(1)
                .resultList
                .firstOrNull()
        }
    }

    fun findMinimalReleaseDateTime(): ZonedDateTime {
        return database.entityManager.use {
            val cb = it.criteriaBuilder
            val query = cb.createQuery(ZonedDateTime::class.java)
            val root = query.from(getEntityClass())
            query.select(cb.least(root[EpisodeMapping_.releaseDateTime]))

            createReadOnlyQuery(it, query)
                .resultList
                .firstOrNull() ?: ZonedDateTime.now()
        }
    }

    fun updateAllReleaseDate() {
        database.inTransaction {
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