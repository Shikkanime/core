package fr.shikkanime.repositories

import fr.shikkanime.dtos.mappings.EpisodeMappingSeoDto
import fr.shikkanime.entities.*
import fr.shikkanime.entities.enums.CountryCode
import fr.shikkanime.entities.enums.EpisodeType
import fr.shikkanime.entities.enums.ImageType
import fr.shikkanime.entities.enums.Platform
import fr.shikkanime.entities.miscellaneous.GroupedEpisode
import fr.shikkanime.entities.miscellaneous.Pageable
import fr.shikkanime.entities.miscellaneous.SortParameter
import fr.shikkanime.utils.Constant
import jakarta.persistence.criteria.JoinType
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
    ): Pageable<EpisodeMapping> {
        return database.entityManager.use { entityManager ->
            val cb = entityManager.criteriaBuilder
            val query = cb.createQuery(getEntityClass())
            val root = query.from(getEntityClass())

            val predicates = mutableListOf<Predicate>()
            anime?.let { predicates.add(cb.equal(root[EpisodeMapping_.anime], it)) }
            season?.let { predicates.add(cb.equal(root[EpisodeMapping_.season], it)) }
            countryCode?.let { predicates.add(cb.equal(root[EpisodeMapping_.anime][Anime_.countryCode], it)) }
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

    fun findAllNeedUpdate(lastDateTime: ZonedDateTime): List<EpisodeMapping> {
        return database.entityManager.use {
            val cb = it.criteriaBuilder
            val query = cb.createQuery(getEntityClass())
            val root = query.from(getEntityClass())

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
                        cb.lessThanOrEqualTo(
                            root[EpisodeMapping_.lastUpdateDateTime],
                            lastDateTime
                        ),
                        cb.and(
                            cb.equal(attachmentSubquery, 1L),
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

    private fun ZonedDateTime.truncateToHour(): ZonedDateTime {
        return withMinute(0).withSecond(0).withNano(0)
    }

    fun findAllGroupedBy(countryCode: CountryCode?, page: Int, limit: Int): Pageable<GroupedEpisode> {
        return database.entityManager.use { entityManager ->
            val cb = entityManager.criteriaBuilder

            val subQuery = cb.createQuery(Array::class.java)
            val subRoot = subQuery.from(EpisodeVariant::class.java)
            val subMapping = subRoot.join(EpisodeVariant_.mapping)
            val subAnime = subMapping.join(EpisodeMapping_.anime)

            val truncatedReleaseDateTime = cb.function(
                "date_trunc",
                ZonedDateTime::class.java,
                cb.literal("hour"),
                subRoot[EpisodeVariant_.releaseDateTime]
            )

            subQuery.multiselect(
                subAnime[Anime_.uuid],
                subMapping[EpisodeMapping_.episodeType],
                truncatedReleaseDateTime
            )

            countryCode?.let { subQuery.where(cb.equal(subAnime[Anime_.countryCode], it)) }

            subQuery.groupBy(
                subAnime[Anime_.uuid],
                subMapping[EpisodeMapping_.episodeType],
                truncatedReleaseDateTime
            )

            subQuery.orderBy(
                cb.desc(
                    truncatedReleaseDateTime
                )
            )

            val pageableSubquery = buildPageableQuery(createReadOnlyQuery(entityManager, subQuery), page, limit)

            val query = cb.createQuery(EpisodeVariant::class.java)
            val root = query.from(EpisodeVariant::class.java)

            // Fetch mapping and anime eagerly
            root.fetch(EpisodeVariant_.mapping, JoinType.INNER)
                .fetch(EpisodeMapping_.anime, JoinType.INNER)

            val inPredicate = cb.or(
                *pageableSubquery.data.map { result ->
                    cb.and(
                        cb.equal(root[EpisodeVariant_.mapping][EpisodeMapping_.anime][Anime_.uuid], result[0]),
                        cb.equal(root[EpisodeVariant_.mapping][EpisodeMapping_.episodeType], result[1]),
                        cb.equal(cb.function("date_trunc", ZonedDateTime::class.java, cb.literal("hour"), root[EpisodeVariant_.releaseDateTime]), result[2])
                    )
                }.toTypedArray()
            )

            query.where(inPredicate)

            query.orderBy(
                cb.desc(root[EpisodeVariant_.releaseDateTime]),
                cb.desc(root[EpisodeVariant_.mapping][EpisodeMapping_.anime][Anime_.name]),
                cb.desc(root[EpisodeVariant_.mapping][EpisodeMapping_.season]),
                cb.desc(root[EpisodeVariant_.mapping][EpisodeMapping_.episodeType]),
                cb.desc(root[EpisodeVariant_.mapping][EpisodeMapping_.number])
            )

            val groups = createReadOnlyQuery(entityManager, query).resultList
                .groupBy { episodeVariant -> "${episodeVariant.mapping!!.anime!!.uuid}-${episodeVariant.mapping!!.episodeType}-${episodeVariant.releaseDateTime.truncateToHour()}" }
                .map { (_, variants) ->
                    val firstVariant = variants.first()
                    val firstMapping = firstVariant.mapping!!
                    val anime = firstMapping.anime!!
                    val episodeType = firstMapping.episodeType!!
                    val releaseDateTime = firstVariant.releaseDateTime.truncateToHour()

                    val mappingUuids = variants.asSequence()
                        .map { it.mapping!! }
                        .sortedWith(compareBy({it.season}, {it.episodeType!!}, {it.number}))
                        .map { it.uuid!! }
                        .toSet()
                    val isSingleMapping = mappingUuids.size == 1

                    GroupedEpisode(
                        anime = anime,
                        releaseDateTime = releaseDateTime,
                        lastUpdateDateTime = variants.maxOf { it.mapping!!.lastUpdateDateTime },
                        minSeason = variants.minOf { it.mapping!!.season!! },
                        maxSeason = variants.maxOf { it.mapping!!.season!! },
                        episodeType = episodeType,
                        minNumber = variants.minOf { it.mapping!!.number!! },
                        maxNumber = variants.maxOf { it.mapping!!.number!! },
                        platforms = variants.map { it.platform!! }.toSet(),
                        audioLocales = variants.map { it.audioLocale!! }.toSet(),
                        urls = variants.map { it.url!! }.toSet(),
                        mappings = mappingUuids,
                        title = if (isSingleMapping) firstMapping.title else null,
                        description = if (isSingleMapping) firstMapping.description else null,
                        duration = if (isSingleMapping) firstMapping.duration else null
                    )
                }.toSet()

            Pageable(
                data = groups,
                page = pageableSubquery.page,
                limit = pageableSubquery.limit,
                total = pageableSubquery.total
            )
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