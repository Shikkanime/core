package fr.shikkanime.repositories

import com.google.inject.Inject
import fr.shikkanime.entities.*
import fr.shikkanime.entities.enums.CountryCode
import fr.shikkanime.entities.enums.Platform
import fr.shikkanime.services.MemberFollowAnimeService
import jakarta.persistence.Tuple
import java.time.ZonedDateTime
import java.util.*

class EpisodeVariantRepository : AbstractRepository<EpisodeVariant>() {
    @Inject
    private lateinit var memberFollowAnimeService: MemberFollowAnimeService

    override fun getEntityClass() = EpisodeVariant::class.java

    fun findAllAnimeEpisodeMappingReleaseDateTimePlatformAudioLocaleByDateRange(
        member: Member?,
        countryCode: CountryCode,
        start: ZonedDateTime,
        end: ZonedDateTime,
    ): List<Tuple> {
        return database.entityManager.use { entityManager ->
            val cb = entityManager.criteriaBuilder
            val query = cb.createTupleQuery()
            val root = query.from(getEntityClass())

            query.multiselect(
                    root[EpisodeVariant_.mapping][EpisodeMapping_.anime],
                    root[EpisodeVariant_.mapping],
                    root[EpisodeVariant_.releaseDateTime],
                    root[EpisodeVariant_.platform],
                    root[EpisodeVariant_.audioLocale],
                )

            val countryPredicate =
                cb.equal(root[EpisodeVariant_.mapping][EpisodeMapping_.anime][Anime_.countryCode], countryCode)
            val datePredicate = cb.between(root[EpisodeVariant_.releaseDateTime], start, end)
            val predicates = mutableListOf(countryPredicate, datePredicate)

            member?.let {
                val animePredicate = root[EpisodeVariant_.mapping][EpisodeMapping_.anime][Anime_.uuid].`in`(
                    memberFollowAnimeService.findAllFollowedAnimesUUID(it)
                )
                predicates.add(animePredicate)
            }

            query.where(cb.and(*predicates.toTypedArray()))

            query.orderBy(
                cb.asc(root[EpisodeVariant_.releaseDateTime]),
                cb.asc(root[EpisodeVariant_.mapping][EpisodeMapping_.season]),
                cb.asc(root[EpisodeVariant_.mapping][EpisodeMapping_.episodeType]),
                cb.asc(root[EpisodeVariant_.mapping][EpisodeMapping_.number])
            )

            createReadOnlyQuery(entityManager, query)
                .resultList
        }
    }

    fun findAllIdentifierByDateRangeWithoutNextEpisode(
        countryCode: CountryCode,
        start: ZonedDateTime,
        end: ZonedDateTime,
        platform: Platform
    ): List<String> {
        return database.entityManager.use { entityManager ->
            val cb = entityManager.criteriaBuilder
            val query = cb.createQuery(String::class.java)
            val root = query.from(getEntityClass())

            val mappingJoin = root.join(EpisodeVariant_.mapping)
            val animeJoin = mappingJoin.join(EpisodeMapping_.anime)

            query.select(root[EpisodeVariant_.identifier])

            val countryPredicate = cb.equal(animeJoin[Anime_.countryCode], countryCode)
            val datePredicate = cb.between(root[EpisodeVariant_.releaseDateTime], start, end)
            val platformPredicate = cb.equal(root[EpisodeVariant_.platform], platform)

            val subQuery = query.subquery(UUID::class.java)
            val subRoot = subQuery.from(getEntityClass())
            val subMappingJoin = subRoot.join(EpisodeVariant_.mapping)

            subQuery.select(subMappingJoin[EpisodeMapping_.uuid])
            subQuery.where(
                cb.and(
                    cb.equal(subMappingJoin[EpisodeMapping_.anime], mappingJoin[EpisodeMapping_.anime]),
                    cb.equal(subMappingJoin[EpisodeMapping_.season], mappingJoin[EpisodeMapping_.season]),
                    cb.or(
                        cb.greaterThan(subMappingJoin[EpisodeMapping_.lastReleaseDateTime], mappingJoin[EpisodeMapping_.lastReleaseDateTime]),
                        cb.and(
                            cb.equal(subMappingJoin[EpisodeMapping_.episodeType], mappingJoin[EpisodeMapping_.episodeType]),
                            cb.greaterThan(subMappingJoin[EpisodeMapping_.number], mappingJoin[EpisodeMapping_.number])
                        )
                    ),
                    cb.notEqual(subMappingJoin[EpisodeMapping_.uuid], mappingJoin[EpisodeMapping_.uuid])
                )
            )

            query.where(
                cb.and(
                    countryPredicate,
                    datePredicate,
                    platformPredicate,
                    cb.not(cb.exists(subQuery))
                )
            )

            createReadOnlyQuery(entityManager, query)
                .resultList
        }
    }

    fun findAllTypeIdentifier(): List<Tuple> {
        return database.entityManager.use {
            val cb = it.criteriaBuilder
            val query = cb.createTupleQuery()
            val root = query.from(getEntityClass())

            val episodeMappingPath = root[EpisodeVariant_.mapping]
            val animePath = episodeMappingPath[EpisodeMapping_.anime]

            query.multiselect(
                animePath[Anime_.countryCode],
                animePath[Anime_.uuid],
                root[EpisodeVariant_.platform],
                episodeMappingPath[EpisodeMapping_.episodeType],
                episodeMappingPath[EpisodeMapping_.season],
                episodeMappingPath[EpisodeMapping_.number],
                root[EpisodeVariant_.audioLocale],
                root[EpisodeVariant_.identifier]
            )

            createReadOnlyQuery(it, query)
                .resultList
        }
    }

    fun findAllIdentifiers(): List<String> {
        return database.entityManager.use {
            val cb = it.criteriaBuilder
            val query = cb.createQuery(String::class.java)
            val root = query.from(getEntityClass())

            query.select(root[EpisodeVariant_.identifier])
                .distinct(true)

            createReadOnlyQuery(it, query)
                .resultList
        }
    }

    fun findAudioLocalesAndSeasonsByAnime(anime: Anime): Pair<List<String>, List<Pair<Int, ZonedDateTime>>> {
        return database.entityManager.use { em ->
            val cb = em.criteriaBuilder
            val query = cb.createTupleQuery()

            val variantRoot = query.from(getEntityClass())
            val mappingJoin = variantRoot.join(EpisodeVariant_.mapping)

            query.multiselect(
                variantRoot[EpisodeVariant_.audioLocale],
                mappingJoin[EpisodeMapping_.season],
                cb.greatest(mappingJoin[EpisodeMapping_.lastReleaseDateTime])
            )

            query.where(cb.equal(mappingJoin[EpisodeMapping_.anime], anime))
            query.groupBy(
                variantRoot[EpisodeVariant_.audioLocale],
                mappingJoin[EpisodeMapping_.season]
            )
            query.orderBy(cb.asc(mappingJoin[EpisodeMapping_.season]))

            val results = createReadOnlyQuery(em, query).resultList

            // Process results
            val audioLocales = mutableSetOf<String>()
            val seasons = mutableListOf<Pair<Int, ZonedDateTime>>()

            results.forEach { tuple ->
                audioLocales.add(tuple[0] as String)
                seasons.add(tuple[1] as Int to tuple[2] as ZonedDateTime)
            }

            Pair(audioLocales.toList(), seasons.distinctBy { it.first })
        }
    }

    fun findAllByAnime(anime: Anime): List<EpisodeVariant> {
        return database.entityManager.use {
            val cb = it.criteriaBuilder
            val query = cb.createQuery(getEntityClass())
            val root = query.from(getEntityClass())

            query.where(cb.equal(root[EpisodeVariant_.mapping][EpisodeMapping_.anime], anime))

            createReadOnlyQuery(it, query)
                .resultList
        }
    }

    fun findAllByMapping(mapping: EpisodeMapping): List<EpisodeVariant> {
        return database.entityManager.use {
            val cb = it.criteriaBuilder
            val query = cb.createQuery(getEntityClass())
            val root = query.from(getEntityClass())

            query.where(cb.equal(root[EpisodeVariant_.mapping], mapping))

            createReadOnlyQuery(it, query)
                .resultList
        }
    }

    fun findByIdentifier(identifier: String): EpisodeVariant? {
        return database.entityManager.use {
            val cb = it.criteriaBuilder
            val query = cb.createQuery(getEntityClass())
            val root = query.from(getEntityClass())

            query.where(cb.equal(root[EpisodeVariant_.identifier], identifier))

            createReadOnlyQuery(it, query)
                .resultList
                .firstOrNull()
        }
    }
}