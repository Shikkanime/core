package fr.shikkanime.repositories

import fr.shikkanime.entities.Anime_
import fr.shikkanime.entities.EpisodeMapping_
import fr.shikkanime.entities.EpisodeVariant
import fr.shikkanime.entities.EpisodeVariant_
import fr.shikkanime.entities.enums.CountryCode
import fr.shikkanime.entities.enums.EpisodeType
import fr.shikkanime.entities.enums.Platform
import fr.shikkanime.utils.indexers.GroupedIndexer
import fr.shikkanime.utils.indexers.NewGroupIndexer
import jakarta.persistence.Tuple
import jakarta.persistence.criteria.JoinType
import java.time.ZonedDateTime
import java.util.*

class EpisodeVariantRepository : AbstractRepository<EpisodeVariant>() {
    suspend fun preIndex() {
        dispatch {
            val cb = it.criteriaBuilder

            val query = cb.createTupleQuery().apply {
                val root = from(entityClass)
                distinct(true)

                val episodeMappingRoot = root[EpisodeVariant_.mapping]
                val animeRoot = episodeMappingRoot[EpisodeMapping_.anime]

                select(
                    cb.tuple(
                        animeRoot[Anime_.countryCode],
                        animeRoot[Anime_.uuid],
                        animeRoot[Anime_.slug],
                        episodeMappingRoot[EpisodeMapping_.uuid],
                        episodeMappingRoot[EpisodeMapping_.season],
                        episodeMappingRoot[EpisodeMapping_.episodeType],
                        episodeMappingRoot[EpisodeMapping_.number],
                        root[EpisodeVariant_.uuid],
                        root[EpisodeVariant_.releaseDateTime],
                        root[EpisodeVariant_.audioLocale]
                    )
                )

                orderBy(
                    cb.asc(animeRoot[Anime_.countryCode]),
                    cb.asc(animeRoot[Anime_.slug]),
                    cb.asc(episodeMappingRoot[EpisodeMapping_.episodeType]),
                    cb.asc(root[EpisodeVariant_.releaseDateTime]),
                    cb.desc(root[EpisodeVariant_.audioLocale])
                )
            }

            createReadOnlyQuery(it, query).resultStream.forEach { tuple ->
                val countryCode = tuple[0, CountryCode::class.java]
                val animeUuid = tuple[1, UUID::class.java]
                val animeSlug = tuple[2, String::class.java]
                val episodeMappingUuid = tuple[3, UUID::class.java]
                val season = tuple[4, Int::class.java]
                val episodeType = tuple[5, EpisodeType::class.java]
                val number = tuple[6, Int::class.java]
                val variantUuid = tuple[7, UUID::class.java]
                val releaseDateTime = tuple[8, ZonedDateTime::class.java]
                val audioLocale = tuple[9, String::class.java]

                GroupedIndexer.add(
                    GroupedIndexer.CompositeKey(
                        countryCode,
                        animeUuid,
                        animeSlug,
                        episodeType
                    ),
                    variantUuid,
                    episodeMappingUuid,
                    releaseDateTime,
                    audioLocale
                )

                NewGroupIndexer.addToBucket(
                    countryCode,
                    animeUuid,
                    animeSlug,
                    season,
                    episodeType,
                    number,
                    variantUuid,
                    releaseDateTime,
                    audioLocale
                )
            }
        }

        NewGroupIndexer.buildIndex()
    }

    override suspend fun findAll(): List<EpisodeVariant> {
        return dispatch {
            val cb = it.criteriaBuilder
            val query = cb.createQuery(entityClass)

            query.from(entityClass)
                .fetch(EpisodeVariant_.mapping, JoinType.INNER)
                .fetch(EpisodeMapping_.anime, JoinType.INNER)

            createReadOnlyQuery(it, query)
                .resultList
        }
    }

    suspend fun findAllTypeIdentifier(): List<Tuple> {
        return dispatch {
            val cb = it.criteriaBuilder
            val query = cb.createTupleQuery()
            val root = query.from(entityClass)

            query.distinct(true)
                .select(
                    cb.tuple(
                        root[EpisodeVariant_.mapping][EpisodeMapping_.anime][Anime_.countryCode],
                        root[EpisodeVariant_.mapping][EpisodeMapping_.anime][Anime_.uuid],
                        root[EpisodeVariant_.mapping][EpisodeMapping_.season],
                        root[EpisodeVariant_.mapping][EpisodeMapping_.episodeType],
                        root[EpisodeVariant_.mapping][EpisodeMapping_.number],
                        root[EpisodeVariant_.audioLocale]
                    )
                )

            createReadOnlyQuery(it, query)
                .resultList
        }
    }

    suspend fun findAllByAnime(animeUuid: UUID): List<EpisodeVariant> {
        return dispatch {
            val cb = it.criteriaBuilder
            val query = cb.createQuery(entityClass)
            val root = query.from(entityClass)

            query.where(cb.equal(root[EpisodeVariant_.mapping][EpisodeMapping_.anime][Anime_.uuid], animeUuid))

            createReadOnlyQuery(it, query)
                .resultList
        }
    }

    suspend fun findAllByMapping(mappingUUID: UUID): List<EpisodeVariant> {
        return dispatch {
            val cb = it.criteriaBuilder
            val query = cb.createQuery(entityClass)
            val root = query.from(entityClass)

            query.where(cb.equal(root[EpisodeVariant_.mapping][EpisodeMapping_.uuid], mappingUUID))

            createReadOnlyQuery(it, query)
                .resultList
        }
    }

    suspend fun findAllIdentifiersByMappingsAndPlatform(mappingUuids: Collection<UUID>, platform: Platform): List<String> {
        return dispatch {
            val cb = it.criteriaBuilder
            val query = cb.createQuery(String::class.java)
            val root = query.from(entityClass)

            query.select(root[EpisodeVariant_.identifier])

            query.where(
                cb.and(
                    root[EpisodeVariant_.mapping][EpisodeMapping_.uuid].`in`(mappingUuids),
                    cb.equal(root[EpisodeVariant_.platform], platform)
                )
            )

            createReadOnlyQuery(it, query)
                .resultList
        }
    }

    suspend fun findAllIdentifiers(): HashSet<String> {
        return dispatch {
            val cb = it.criteriaBuilder
            val query = cb.createQuery(String::class.java)
            val root = query.from(entityClass)

            query.select(root[EpisodeVariant_.identifier])

            createReadOnlyQuery(it, query)
                .resultList
                .toHashSet()
        }
    }

    suspend fun findAllVariantsByCountryCodeAndPlatformAndReleaseDateTimeBetween(
        countryCode: CountryCode,
        platform: Platform,
        startZonedDateTime: ZonedDateTime,
        endZonedDateTime: ZonedDateTime
    ): List<Pair<String, ZonedDateTime>> {
        return dispatch {
            val query = it.createQuery("""
                SELECT ev.identifier, ev.releaseDateTime
                FROM EpisodeVariant ev
                    JOIN ev.mapping m
                    JOIN m.anime a
                WHERE a.countryCode = :countryCode
                    AND ev.platform = :platform
                    AND ev.releaseDateTime BETWEEN :startZonedDateTime AND :endZonedDateTime
                    AND NOT EXISTS (
                        SELECT 1
                        FROM EpisodeMapping em
                        WHERE em.anime.uuid = a.uuid
                            AND (em.releaseDateTime, em.season, em.episodeType, em.number) > 
                                    (m.releaseDateTime, m.season, m.episodeType, m.number)
                    )
                ORDER BY ev.releaseDateTime ASC
            """.trimIndent(), Tuple::class.java)

            query.setParameter("countryCode", countryCode)
            query.setParameter("platform", platform)
            query.setParameter("startZonedDateTime", startZonedDateTime)
            query.setParameter("endZonedDateTime", endZonedDateTime)

            createReadOnlyQuery(query)
                .resultList
                .map { tuple -> tuple[0, String::class.java] to tuple[1, ZonedDateTime::class.java] }
        }
    }

    suspend fun findAllByMappingAndPlatformAndAudioLocaleAndUncensored(episodeMappingUuid: UUID, platform: Platform, audioLocale: String, uncensored: Boolean): List<EpisodeVariant> {
        return dispatch {
            val cb = it.criteriaBuilder
            val query = cb.createQuery(entityClass)
            val root = query.from(entityClass)

            query.where(
                cb.and(
                    cb.equal(root[EpisodeVariant_.mapping][EpisodeMapping_.uuid], episodeMappingUuid),
                    cb.equal(root[EpisodeVariant_.platform], platform),
                    cb.equal(root[EpisodeVariant_.audioLocale], audioLocale),
                    cb.equal(root[EpisodeVariant_.uncensored], uncensored)
                )
            )

            createReadOnlyQuery(it, query)
                .resultList
        }
    }

    suspend fun findByIdentifier(identifier: String): EpisodeVariant? {
        return dispatch {
            val cb = it.criteriaBuilder
            val query = cb.createQuery(entityClass)
            val root = query.from(entityClass)

            query.where(cb.equal(root[EpisodeVariant_.identifier], identifier))

            createReadOnlyQuery(it, query)
                .resultList
                .firstOrNull()
        }
    }
}