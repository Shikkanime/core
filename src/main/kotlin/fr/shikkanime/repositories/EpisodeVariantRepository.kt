package fr.shikkanime.repositories

import fr.shikkanime.entities.*
import fr.shikkanime.entities.enums.CountryCode
import fr.shikkanime.entities.enums.EpisodeType
import fr.shikkanime.entities.enums.Platform
import jakarta.persistence.Tuple
import jakarta.persistence.criteria.JoinType
import java.time.ZonedDateTime
import java.util.*

class EpisodeVariantRepository : AbstractRepository<EpisodeVariant>() {
    override fun getEntityClass() = EpisodeVariant::class.java

    override fun findAll(): List<EpisodeVariant> {
        return database.entityManager.use {
            val cb = it.criteriaBuilder
            val query = cb.createQuery(getEntityClass())
            query.from(getEntityClass())
                .fetch(EpisodeVariant_.mapping, JoinType.INNER)

            createReadOnlyQuery(it, query)
                .resultList
        }
    }

    fun findAllAnimeEpisodeMappingReleaseDateTimePlatformAudioLocaleByDateRange(
        countryCode: CountryCode,
        member: Member?,
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

            val countryPredicate = cb.equal(root[EpisodeVariant_.mapping][EpisodeMapping_.anime][Anime_.countryCode], countryCode)
            val datePredicate = cb.between(root[EpisodeVariant_.releaseDateTime], start, end)
            val predicates = mutableListOf(countryPredicate, datePredicate)

            member?.let {
                val memberFollowAnimeJoin = root.join(EpisodeVariant_.mapping)
                    .join(EpisodeMapping_.anime)
                    .join(Anime_.followings)

                predicates.add(cb.equal(memberFollowAnimeJoin[MemberFollowAnime_.member], member))
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

    fun findAllSimulcastedByAnime(anime: Anime): List<EpisodeVariant> {
        return database.entityManager.use {
            val cb = it.criteriaBuilder
            val query = cb.createQuery(getEntityClass())
            val root = query.from(getEntityClass())

            query.where(
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
}