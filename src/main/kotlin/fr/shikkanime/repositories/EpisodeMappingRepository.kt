package fr.shikkanime.repositories

import fr.shikkanime.dtos.enums.Status
import fr.shikkanime.dtos.mappings.EpisodeMappingSeoDto
import fr.shikkanime.entities.*
import fr.shikkanime.entities.enums.CountryCode
import fr.shikkanime.entities.enums.EpisodeType
import fr.shikkanime.entities.enums.Platform
import fr.shikkanime.entities.miscellaneous.GroupedEpisode
import fr.shikkanime.entities.miscellaneous.Pageable
import fr.shikkanime.entities.miscellaneous.SortParameter
import fr.shikkanime.utils.Constant
import jakarta.persistence.Tuple
import jakarta.persistence.criteria.Predicate
import java.time.ZonedDateTime
import java.util.*

class EpisodeMappingRepository : AbstractRepository<EpisodeMapping>() {
    override fun getEntityClass() = EpisodeMapping::class.java

    fun findAllUuids(): List<UUID> {
        return database.entityManager.use {
            val cb = it.criteriaBuilder
            val query = cb.createQuery(UUID::class.java)
            val root = query.from(getEntityClass())
            query.select(root[EpisodeMapping_.uuid])
            createReadOnlyQuery(it, query).resultList
        }
    }

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

                val field = when (sortParameter.field) {
                    "episodeType" -> root[EpisodeMapping_.episodeType]
                    "releaseDateTime" -> root[EpisodeMapping_.releaseDateTime]
                    "lastReleaseDateTime" -> root[EpisodeMapping_.lastReleaseDateTime]
                    "season" -> root[EpisodeMapping_.season]
                    "number" -> root[EpisodeMapping_.number]
                    "animeName" -> root[EpisodeMapping_.anime][Anime_.name]
                    else -> null
                }

                field?.let { (if (sortParameter.order == SortParameter.Order.ASC) cb::asc else cb::desc).invoke(it) }
            }

            query.orderBy(orders)
            buildPageableQuery(createReadOnlyQuery(entityManager, query), page, limit)
        }
    }

    fun findAllAnimeUuidImageBannerAndUuidImage(): List<Tuple> {
        return database.entityManager.use {
            val cb = it.criteriaBuilder
            val query = cb.createTupleQuery()
            val root = query.from(getEntityClass())

            query.multiselect(
                root[EpisodeMapping_.anime][Anime_.uuid],
                root[EpisodeMapping_.anime][Anime_.image],
                root[EpisodeMapping_.anime][Anime_.banner],
                root[EpisodeMapping_.uuid],
                root[EpisodeMapping_.image]
            ).orderBy(cb.desc(root[EpisodeMapping_.lastReleaseDateTime]))

            createReadOnlyQuery(it, query)
                .resultList
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

    fun findAllNeedUpdateByPlatforms(platforms: List<Platform>, lastDateTime: ZonedDateTime): List<EpisodeMapping> {
        return database.entityManager.use {
            val cb = it.criteriaBuilder
            val query = cb.createQuery(getEntityClass())
            val root = query.from(getEntityClass())
            val variantJoin = root.join(EpisodeMapping_.variants)

            query.distinct(true)
                .where(
                    variantJoin[EpisodeVariant_.platform].`in`(platforms),
                    cb.or(
                        cb.lessThanOrEqualTo(
                            root[EpisodeMapping_.lastUpdateDateTime],
                            lastDateTime
                        ),
                        cb.and(
                            cb.equal(root[EpisodeMapping_.image], Constant.DEFAULT_IMAGE_PREVIEW),
                            cb.greaterThanOrEqualTo(root[EpisodeMapping_.releaseDateTime], lastDateTime),
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
            val root = query.from(getEntityClass())
            root.fetch(EpisodeMapping_.anime)

            val subQuery = query.subquery(Long::class.java)
            val subRoot = subQuery.from(EpisodeVariant::class.java)

            subQuery.select(cb.literal(1L))
                .where(
                    cb.and(
                        cb.equal(subRoot[EpisodeVariant_.mapping][EpisodeMapping_.uuid], root[EpisodeMapping_.uuid]),
                        cb.notEqual(subRoot[EpisodeVariant_.audioLocale], ignoreAudioLocale)
                    )
                )

            query.where(
                cb.and(
                    cb.not(root[EpisodeMapping_.episodeType].`in`(ignoreEpisodeTypes)),
                    cb.exists(subQuery)
                )
            ).orderBy(
                cb.asc(root[EpisodeMapping_.releaseDateTime]),
                cb.asc(root[EpisodeMapping_.season]),
                cb.asc(root[EpisodeMapping_.episodeType]),
                cb.asc(root[EpisodeMapping_.number])
            )

            createReadOnlyQuery(it, query)
                .resultList
        }
    }

    private fun findAllTitleDescriptionAndDurationByUUIDs(
        uuids: List<UUID>
    ): List<Tuple> {
        return database.entityManager.use {
            val cb = it.criteriaBuilder
            val query = cb.createTupleQuery()
            val root = query.from(getEntityClass())

            query.multiselect(
                root[EpisodeMapping_.uuid],
                root[EpisodeMapping_.title],
                root[EpisodeMapping_.description],
                root[EpisodeMapping_.duration]
            )

            query.where(root[EpisodeMapping_.uuid].`in`(uuids))

            createReadOnlyQuery(it, query)
                .resultList
        }
    }

    fun findAllGrouped(
        countryCode: CountryCode,
        page: Int,
        limit: Int,
    ): Pageable<GroupedEpisode> {
        return database.entityManager.use {
            val query = it.createQuery("""
                WITH grouped_data AS (
                    SELECT a AS anime,
                         MIN(ev.releaseDateTime) AS min_release_date_time,
                         MAX(em.lastUpdateDateTime) AS last_update_date_time,
                         MIN(em.season) AS min_season,
                         MAX(em.season) AS max_season,
                         em.episodeType AS episodeType,
                         MIN(em.number) AS min_number,
                         MAX(em.number) AS max_number,
                         ARRAY_AGG(DISTINCT ev.platform) WITHIN GROUP (ORDER BY ev.platform) AS platforms,
                         ARRAY_AGG(DISTINCT ev.audioLocale) WITHIN GROUP (ORDER BY ev.audioLocale) AS audioLocales,
                         ARRAY_AGG(DISTINCT ev.url) WITHIN GROUP (ORDER BY ev.url) AS urls,
                         ARRAY_AGG(DISTINCT em.uuid) WITHIN GROUP (ORDER BY em.uuid) AS mappings
                    FROM EpisodeVariant ev
                        JOIN ev.mapping em
                        JOIN em.anime a
                    WHERE a.countryCode = :countryCode
                    GROUP BY a,
                            em.episodeType,
                            DATE_TRUNC("hour", ev.releaseDateTime)
                )
                SELECT gd.anime,
                    gd.min_release_date_time,
                    gd.last_update_date_time,
                    gd.min_season,
                    gd.max_season,
                    gd.episodeType,
                    gd.min_number,
                    gd.max_number,
                    gd.platforms,
                    gd.audioLocales,
                    gd.urls,
                    gd.mappings
                FROM grouped_data gd
                ORDER BY gd.min_release_date_time DESC,
                    gd.anime.name DESC,
                    gd.min_season DESC,
                    gd.episodeType DESC,
                    gd.min_number DESC
            """.trimIndent(), Tuple::class.java)

            query.setParameter("countryCode", countryCode)

            val tmpPage = buildPageableQuery(createReadOnlyQuery(query), page, limit)
            val page = Pageable<GroupedEpisode>(
                tmpPage.data.map {
                    GroupedEpisode(
                        it[0, Anime::class.java],
                        it[1, ZonedDateTime::class.java],
                        it[2, ZonedDateTime::class.java],
                        it[3, Int::class.java],
                        it[4, Int::class.java],
                        it[5, EpisodeType::class.java],
                        it[6, Int::class.java],
                        it[7, Int::class.java],
                        it[8, Array::class.java].filterIsInstance<Platform>().toSet(),
                        it[9, Array::class.java].filterIsInstance<String>().toSet(),
                        it[10, Array::class.java].filterIsInstance<String>().toSet(),
                        it[11, Array::class.java].filterIsInstance<UUID>().toSet()
                    )
                }.toSet(),
                tmpPage.page,
                tmpPage.limit,
                tmpPage.total
            )


            val singleMappingEpisodes = page.data.filter { it.mappings.size == 1 }
            val uuids = singleMappingEpisodes.map { it.mappings.first() }
            val titleDescriptionAndDuration = findAllTitleDescriptionAndDurationByUUIDs(uuids).associateBy(
                { it[0, UUID::class.java] },
                { Triple(it[1, String::class.java], it[2, String::class.java], it[3, Long::class.java]) }
            )

            singleMappingEpisodes.forEach { groupedEpisode ->
                val (title, description, duration) = titleDescriptionAndDuration[groupedEpisode.mappings.first()]!!
                groupedEpisode.title = title
                groupedEpisode.description = description
                groupedEpisode.duration = duration
            }

            page
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