package fr.shikkanime.repositories

import fr.shikkanime.entities.Anime_
import fr.shikkanime.entities.EpisodeMapping_
import fr.shikkanime.entities.EpisodeVariant
import fr.shikkanime.entities.EpisodeVariant_
import fr.shikkanime.entities.enums.CountryCode
import fr.shikkanime.entities.enums.EpisodeType
import fr.shikkanime.entities.enums.Platform
import fr.shikkanime.utils.indexers.GroupedIndexer
import jakarta.persistence.Tuple
import jakarta.persistence.criteria.JoinType
import java.time.ZonedDateTime
import java.util.*

class EpisodeVariantRepository : AbstractRepository<EpisodeVariant>() {
    override fun getEntityClass() = EpisodeVariant::class.java

    fun preIndex() {
        database.entityManager.use {
            val cb = it.criteriaBuilder

            val query = cb.createTupleQuery().apply {
                val root = from(getEntityClass())
                distinct(true)

                val episodeMappingRoot = root[EpisodeVariant_.mapping]
                val animeRoot = episodeMappingRoot[EpisodeMapping_.anime]

                select(

                    cb.tuple(
                        animeRoot[Anime_.countryCode],
                        animeRoot[Anime_.uuid],
                        animeRoot[Anime_.slug],
                        episodeMappingRoot[EpisodeMapping_.episodeType],
                        root[EpisodeVariant_.releaseDateTime],
                        root[EpisodeVariant_.uuid],
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

            GroupedIndexer.clear()

            createReadOnlyQuery(it, query).resultStream.forEach { tuple ->
                GroupedIndexer.add(
                    tuple[0, CountryCode::class.java],
                    tuple[1, UUID::class.java],
                    tuple[2, String::class.java],
                    tuple[3, EpisodeType::class.java],
                    tuple[4, ZonedDateTime::class.java],
                    tuple[5, UUID::class.java],
                    tuple[6, String::class.java]
                )
            }
        }
    }

    override fun findAll(): List<EpisodeVariant> {
        return database.entityManager.use {
            val cb = it.criteriaBuilder
            val query = cb.createQuery(getEntityClass())

            query.from(getEntityClass())
                .fetch(EpisodeVariant_.mapping, JoinType.INNER)
                .fetch(EpisodeMapping_.anime, JoinType.INNER)

            createReadOnlyQuery(it, query)
                .resultList
        }
    }

    fun findAllTypeIdentifier(): List<Tuple> {
        return database.entityManager.use {
            val cb = it.criteriaBuilder
            val query = cb.createTupleQuery()
            val root = query.from(getEntityClass())

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

    fun findAllByMapping(mappingUUID: UUID): List<EpisodeVariant> {
        return database.entityManager.use {
            val cb = it.criteriaBuilder
            val query = cb.createQuery(getEntityClass())
            val root = query.from(getEntityClass())

            query.where(cb.equal(root[EpisodeVariant_.mapping][EpisodeMapping_.uuid], mappingUUID))

            createReadOnlyQuery(it, query)
                .resultList
        }
    }

    fun findAllIdentifiers(): HashSet<String> {
        return database.entityManager.use {
            val cb = it.criteriaBuilder
            val query = cb.createQuery(String::class.java)
            val root = query.from(getEntityClass())

            query.select(root[EpisodeVariant_.identifier])

            createReadOnlyQuery(it, query)
                .resultList
                .toHashSet()
        }
    }

    fun findAllVariantsByCountryCodeAndPlatformAndReleaseDateTimeBetween(
        countryCode: CountryCode,
        platform: Platform,
        startZonedDateTime: ZonedDateTime,
        endZonedDateTime: ZonedDateTime
    ): List<Pair<String, ZonedDateTime>> {
        return database.entityManager.use {
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
}