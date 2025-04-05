package fr.shikkanime.repositories

import fr.shikkanime.dtos.variants.VariantReleaseDto
import fr.shikkanime.entities.*
import fr.shikkanime.entities.enums.CountryCode
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
                .multiselect(
                    root[EpisodeVariant_.mapping][EpisodeMapping_.anime][Anime_.countryCode],
                    root[EpisodeVariant_.mapping][EpisodeMapping_.anime][Anime_.uuid],
                    root[EpisodeVariant_.mapping][EpisodeMapping_.season],
                    root[EpisodeVariant_.mapping][EpisodeMapping_.episodeType],
                    root[EpisodeVariant_.mapping][EpisodeMapping_.number],
                    root[EpisodeVariant_.audioLocale],
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

    fun findAllVariantReleases(
        countryCode: CountryCode,
        member: Member?,
        startZonedDateTime: ZonedDateTime,
        endZonedDateTime: ZonedDateTime
    ): List<VariantReleaseDto> {
        return database.entityManager.use {
            val cb = it.criteriaBuilder
            val query = cb.createQuery(VariantReleaseDto::class.java)
            val root = query.from(Anime::class.java)
            val mappingsJoin = root.join(Anime_.mappings)
            val variantsJoin = mappingsJoin.join(EpisodeMapping_.variants)

            query.select(
                cb.construct(
                    VariantReleaseDto::class.java,
                    root,
                    mappingsJoin,
                    variantsJoin[EpisodeVariant_.releaseDateTime],
                    variantsJoin[EpisodeVariant_.platform],
                    variantsJoin[EpisodeVariant_.audioLocale],
                )
            )

            val predicate = mutableListOf(cb.equal(root[Anime_.countryCode], countryCode), cb.between(variantsJoin[EpisodeVariant_.releaseDateTime], startZonedDateTime, endZonedDateTime))

            member?.let {
                val followedJoin = root.join(Anime_.followings)
                val members = followedJoin.join(MemberFollowAnime_.member)
                predicate.add(cb.equal(members, it))
            }

            query.where(*predicate.toTypedArray())

            query.orderBy(
                cb.asc(variantsJoin[EpisodeVariant_.releaseDateTime]),
                cb.asc(mappingsJoin[EpisodeMapping_.season]),
                cb.asc(mappingsJoin[EpisodeMapping_.episodeType]),
                cb.asc(mappingsJoin[EpisodeMapping_.number])
            )

            createReadOnlyQuery(it, query)
                .resultList
        }
    }

    fun findAllIdentifiers(): Set<String> {
        return database.entityManager.use {
            val cb = it.criteriaBuilder
            val query = cb.createQuery(String::class.java)
            val root = query.from(getEntityClass())

            query.select(root[EpisodeVariant_.identifier])

            createReadOnlyQuery(it, query)
                .resultList
                .toSet()
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
                .map { it[0] as String to it[1] as ZonedDateTime }
        }
    }
}