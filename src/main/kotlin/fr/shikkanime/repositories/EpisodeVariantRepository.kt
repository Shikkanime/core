package fr.shikkanime.repositories

import com.google.inject.Inject
import fr.shikkanime.entities.*
import fr.shikkanime.entities.enums.CountryCode
import fr.shikkanime.entities.enums.EpisodeType
import fr.shikkanime.services.MemberFollowAnimeService
import jakarta.persistence.Tuple
import java.time.ZonedDateTime

class EpisodeVariantRepository : AbstractRepository<EpisodeVariant>() {
    @Inject
    private lateinit var memberFollowAnimeService: MemberFollowAnimeService

    override fun getEntityClass() = EpisodeVariant::class.java

    fun findAllByDateRange(
        member: Member?,
        countryCode: CountryCode,
        start: ZonedDateTime,
        end: ZonedDateTime,
    ): List<EpisodeVariant> {
        return database.entityManager.use { entityManager ->
            val cb = entityManager.criteriaBuilder
            val query = cb.createQuery(getEntityClass())
            val root = query.from(getEntityClass())

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

    fun findAllAudioLocalesByMapping(mapping: EpisodeMapping): List<String> {
        return database.entityManager.use {
            val cb = it.criteriaBuilder
            val query = cb.createQuery(String::class.java)
            val root = query.from(getEntityClass())

            query.select(root[EpisodeVariant_.audioLocale])
                .where(cb.equal(root[EpisodeVariant_.mapping], mapping))
                .distinct(true)

            createReadOnlyQuery(it, query)
                .resultList
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

    fun findAllSimulcastedByAnime(anime: Anime): List<EpisodeMapping> {
        return database.entityManager.use {
            val cb = it.criteriaBuilder
            val query = cb.createQuery(EpisodeMapping::class.java)
            val root = query.from(getEntityClass())

            query.distinct(true)
                .select(root[EpisodeVariant_.mapping])
                .where(
                    cb.and(
                        cb.notEqual(root[EpisodeVariant_.audioLocale], anime.countryCode!!.locale),
                        cb.notEqual(root[EpisodeVariant_.mapping][EpisodeMapping_.episodeType], EpisodeType.FILM),
                        cb.notEqual(root[EpisodeVariant_.mapping][EpisodeMapping_.episodeType], EpisodeType.SUMMARY),
                        cb.equal(root[EpisodeVariant_.mapping][EpisodeMapping_.anime], anime)
                    )
                )
                .orderBy(
                    cb.asc(root[EpisodeVariant_.mapping][EpisodeMapping_.releaseDateTime]),
                    cb.asc(root[EpisodeVariant_.mapping][EpisodeMapping_.season]),
                    cb.asc(root[EpisodeVariant_.mapping][EpisodeMapping_.episodeType]),
                    cb.asc(root[EpisodeVariant_.mapping][EpisodeMapping_.number]),
                )

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

    fun findMinAndMaxReleaseDateTimeByMapping(mapping: EpisodeMapping): Pair<ZonedDateTime, ZonedDateTime> {
        return database.entityManager.use { entityManager ->
            val cb = entityManager.criteriaBuilder
            val query = cb.createQuery(Tuple::class.java)
            val root = query.from(getEntityClass())

            query.multiselect(
                cb.least(root[EpisodeVariant_.releaseDateTime]),
                cb.greatest(root[EpisodeVariant_.releaseDateTime]),
            )
                .where(cb.equal(root[EpisodeVariant_.mapping], mapping))

            createReadOnlyQuery(entityManager, query)
                .singleResult
                .let { it[0] as ZonedDateTime to it[1] as ZonedDateTime }
        }
    }

    fun findMinAndMaxReleaseDateTimeByAnime(anime: Anime): Pair<ZonedDateTime, ZonedDateTime> {
        return database.entityManager.use { entityManager ->
            val cb = entityManager.criteriaBuilder
            val query = cb.createQuery(Tuple::class.java)
            val root = query.from(getEntityClass())

            query.multiselect(
                cb.least(root[EpisodeVariant_.releaseDateTime]),
                cb.greatest(root[EpisodeVariant_.releaseDateTime]),
            )
                .where(cb.equal(root[EpisodeVariant_.mapping][EpisodeMapping_.anime], anime))

            createReadOnlyQuery(entityManager, query)
                .singleResult
                .let { it[0] as ZonedDateTime to it[1] as ZonedDateTime }
        }
    }
}