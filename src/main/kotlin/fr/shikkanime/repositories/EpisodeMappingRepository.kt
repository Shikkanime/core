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
import jakarta.persistence.EntityManager
import jakarta.persistence.criteria.CriteriaBuilder
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

    fun findAllNeedUpdate(lastUpdateDateTime: ZonedDateTime, lastImageUpdateDateTime: ZonedDateTime): List<EpisodeMapping> {
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
                            lastUpdateDateTime
                        ),
                        cb.and(
                            cb.equal(attachmentSubquery, 1L),
                            cb.greaterThanOrEqualTo(root[EpisodeMapping_.releaseDateTime], lastImageUpdateDateTime),
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

    private data class DailyMaxReleaseInfo(
        val animeUuid: UUID,
        val episodeType: EpisodeType,
        val releaseDay: ZonedDateTime,
        val maxReleaseTime: ZonedDateTime
    )

    /**
     * Finds all grouped episodes by country code, page and limit.
     *
     * A grouped episode is a collection of episodes that are released at the same time (within a 2-hour window),
     * of the same type (e.g., EPISODE, FILM), and for the same anime.
     *
     * This function is paginated and the total is the number of groups.
     *
     * @param countryCode The country code to filter by. If null, all countries are included.
     * @param page The page to get.
     * @param limit The number of items to get.
     * @return A pageable of grouped episodes.
     */
    fun findAllGroupedBy(countryCode: CountryCode?, page: Int, limit: Int): Pageable<GroupedEpisode> {
        return database.entityManager.use { entityManager ->
            val criteriaBuilder = entityManager.criteriaBuilder
            val dailyMaxReleases = findDailyMaxReleases(entityManager, criteriaBuilder, countryCode, page, limit)

            if (dailyMaxReleases.data.isEmpty()) {
                return@use Pageable(emptySet(), page, limit, 0)
            }

            val episodeVariants = findEpisodeVariants(entityManager, criteriaBuilder, dailyMaxReleases.data)
            val groupedEpisodes = groupEpisodeVariants(episodeVariants)

            Pageable(
                data = groupedEpisodes,
                page = dailyMaxReleases.page,
                limit = dailyMaxReleases.limit,
                total = dailyMaxReleases.total,
            )
        }
    }

    private fun findDailyMaxReleases(
        entityManager: EntityManager,
        criteriaBuilder: CriteriaBuilder,
        countryCode: CountryCode?,
        page: Int,
        limit: Int
    ): Pageable<DailyMaxReleaseInfo> {
        val dailyMaxReleaseQuery = criteriaBuilder.createQuery(DailyMaxReleaseInfo::class.java)
        val episodeVariantRoot = dailyMaxReleaseQuery.from(EpisodeVariant::class.java)

        val variantReleaseDateTimeExpression = episodeVariantRoot[EpisodeVariant_.releaseDateTime]
        val greatestReleaseDate = criteriaBuilder.greatest(variantReleaseDateTimeExpression)
        val episodeMappingRoot = episodeVariantRoot[EpisodeVariant_.mapping]
        val animeRoot = episodeMappingRoot[EpisodeMapping_.anime]
        val animeUuidExpression = animeRoot[Anime_.uuid]
        val episodeTypeExpression = episodeMappingRoot[EpisodeMapping_.episodeType]

        val releaseDayExpression = criteriaBuilder.function(
            "date_trunc",
            ZonedDateTime::class.java,
            criteriaBuilder.literal("day"),
            variantReleaseDateTimeExpression
        ).`as`(ZonedDateTime::class.java)

        dailyMaxReleaseQuery.select(
            criteriaBuilder.construct(
                DailyMaxReleaseInfo::class.java,
                animeUuidExpression,
                episodeTypeExpression,
                releaseDayExpression,
                greatestReleaseDate
            )
        )

        countryCode?.let {
            dailyMaxReleaseQuery.where(
                criteriaBuilder.equal(
                    animeRoot[Anime_.countryCode],
                    it
                )
            )
        }

        dailyMaxReleaseQuery.groupBy(
            animeUuidExpression,
            episodeTypeExpression,
            releaseDayExpression
        )

        dailyMaxReleaseQuery.orderBy(criteriaBuilder.desc(greatestReleaseDate))

        return buildPageableQuery(createReadOnlyQuery(entityManager, dailyMaxReleaseQuery), page, limit)
    }

    private fun findEpisodeVariants(
        entityManager: EntityManager,
        criteriaBuilder: CriteriaBuilder,
        dailyMaxReleases: Set<DailyMaxReleaseInfo>
    ): List<EpisodeVariant> {
        val mainQuery = criteriaBuilder.createQuery(EpisodeVariant::class.java)
        val episodeVariantRoot = mainQuery.from(EpisodeVariant::class.java)

        episodeVariantRoot.fetch(EpisodeVariant_.mapping, JoinType.INNER)
            .fetch(EpisodeMapping_.anime, JoinType.INNER)

        val variantReleaseDateTimeExpression = episodeVariantRoot[EpisodeVariant_.releaseDateTime]
        val episodeMappingRoot = episodeVariantRoot[EpisodeVariant_.mapping]
        val episodeTypeExpression = episodeMappingRoot[EpisodeMapping_.episodeType]
        val animeRoot = episodeMappingRoot[EpisodeMapping_.anime]
        val animeUuidExpression = animeRoot[Anime_.uuid]
        val releaseDateTruncated = criteriaBuilder.function(
            "date_trunc",
            ZonedDateTime::class.java,
            criteriaBuilder.literal("day"),
            variantReleaseDateTimeExpression
        )

        val predicates = dailyMaxReleases.map { dailyMaxRelease ->
            val animePredicate = criteriaBuilder.equal(animeUuidExpression, dailyMaxRelease.animeUuid)
            val episodeTypePredicate = criteriaBuilder.equal(episodeTypeExpression, dailyMaxRelease.episodeType)
            val releaseDayPredicate = criteriaBuilder.equal(releaseDateTruncated, dailyMaxRelease.releaseDay)

            val timeWindowPredicate = criteriaBuilder.between(
                variantReleaseDateTimeExpression,
                dailyMaxRelease.maxReleaseTime.minusHours(1),
                dailyMaxRelease.maxReleaseTime.plusHours(1)
            )

            criteriaBuilder.and(animePredicate, episodeTypePredicate, releaseDayPredicate, timeWindowPredicate)
        }

        mainQuery.select(episodeVariantRoot)
        mainQuery.where(criteriaBuilder.or(*predicates.toTypedArray()))
        mainQuery.orderBy(criteriaBuilder.desc(variantReleaseDateTimeExpression))

        return createReadOnlyQuery(entityManager, mainQuery).resultList
    }

    private fun groupEpisodeVariants(variants: List<EpisodeVariant>): Set<GroupedEpisode> {
        val groups = mutableMapOf<Pair<String, ZonedDateTime>, UUID>()

        val groupedByAnimeAndType = variants.groupBy {
            val mapping = it.mapping
            "${mapping?.anime?.uuid}-${mapping?.episodeType}"
        }

        return groupedByAnimeAndType.flatMap { (_, variantsForAnimeType) ->
            variantsForAnimeType.groupBy { variant ->
                val animeTypeKey = "${variant.mapping?.anime?.uuid}-${variant.mapping?.episodeType}"

                val groupKey = groups.entries.find { (key, _) ->
                    val (existingAnimeTypeKey, releaseDateTime) = key
                    animeTypeKey == existingAnimeTypeKey && variant.releaseDateTime in releaseDateTime.minusHours(1)..releaseDateTime.plusHours(1)
                }

                groupKey?.value ?: UUID.randomUUID().also { groups[animeTypeKey to variant.releaseDateTime] = it }
            }.values
        }.map(::toGroupedEpisode).toSet()
    }

    private fun toGroupedEpisode(variants: List<EpisodeVariant>): GroupedEpisode {
        val firstVariant = variants.first()
        val firstMapping = firstVariant.mapping!!

        val mappingUuids = variants.asSequence()
            .map { it.mapping!! }
            .sortedWith(compareBy({ it.season }, { it.episodeType!! }, { it.number }))
            .map { it.uuid!! }
            .toSet()

        val isSingleMapping = mappingUuids.size == 1

        return GroupedEpisode(
            anime = firstMapping.anime!!,
            releaseDateTime = variants.minOf { it.releaseDateTime },
            lastUpdateDateTime = variants.maxOf { it.mapping!!.lastUpdateDateTime },
            minSeason = variants.minOf { it.mapping!!.season!! },
            maxSeason = variants.maxOf { it.mapping!!.season!! },
            episodeType = firstMapping.episodeType!!,
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