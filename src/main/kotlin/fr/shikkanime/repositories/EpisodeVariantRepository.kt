package fr.shikkanime.repositories

import fr.shikkanime.dtos.variants.VariantReleaseDto
import fr.shikkanime.entities.*
import fr.shikkanime.entities.enums.CountryCode
import fr.shikkanime.entities.enums.EpisodeType
import fr.shikkanime.entities.enums.LangType
import fr.shikkanime.entities.enums.Platform
import fr.shikkanime.utils.indexers.GroupedIndexer
import jakarta.persistence.Tuple
import jakarta.persistence.criteria.JoinType
import jakarta.persistence.criteria.Predicate
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
                select(
                    cb.tuple(
                        root[EpisodeVariant_.mapping][EpisodeMapping_.anime][Anime_.countryCode],
                        root[EpisodeVariant_.mapping][EpisodeMapping_.anime][Anime_.slug],
                        root[EpisodeVariant_.mapping][EpisodeMapping_.episodeType],
                        root[EpisodeVariant_.releaseDateTime],
                        root[EpisodeVariant_.uuid]
                    )
                )
            }

            GroupedIndexer.clear()

            createReadOnlyQuery(it, query).resultStream.forEach { tuple ->
                GroupedIndexer.add(
                    tuple[0, CountryCode::class.java],
                    tuple[1, String::class.java],
                    tuple[2, EpisodeType::class.java],
                    tuple[3, ZonedDateTime::class.java],
                    tuple[4, UUID::class.java]
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

    fun findAllVariantReleases(
        countryCode: CountryCode,
        member: Member?,
        startZonedDateTime: ZonedDateTime,
        endZonedDateTime: ZonedDateTime,
        searchTypes: Array<LangType>? = null,
    ): List<VariantReleaseDto> {
        return database.entityManager.use { entityManager ->
            val cb = entityManager.criteriaBuilder
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

            val predicate = mutableListOf(
                cb.equal(root[Anime_.countryCode], countryCode),
                cb.between(variantsJoin[EpisodeVariant_.releaseDateTime], startZonedDateTime, endZonedDateTime)
            )

            member?.let {
                val followedJoin = root.join(Anime_.followings)
                val members = followedJoin.join(MemberFollowAnime_.member)
                predicate.add(cb.equal(members, it))
            }

            val orPredicate = mutableListOf<Predicate>()

            searchTypes?.let { st ->
                st.forEach { langType ->
                    when (langType) {
                        LangType.SUBTITLES -> orPredicate.add(cb.notEqual(variantsJoin[EpisodeVariant_.audioLocale], countryCode.locale))
                        LangType.VOICE -> orPredicate.add(cb.equal(variantsJoin[EpisodeVariant_.audioLocale], countryCode.locale))
                    }
                }
            }

            query.where(
                *predicate.toTypedArray(),
                if (orPredicate.isNotEmpty()) cb.or(*orPredicate.toTypedArray()) else cb.conjunction()
            )

            query.orderBy(
                cb.asc(variantsJoin[EpisodeVariant_.releaseDateTime]),
                cb.asc(mappingsJoin[EpisodeMapping_.season]),
                cb.asc(mappingsJoin[EpisodeMapping_.episodeType]),
                cb.asc(mappingsJoin[EpisodeMapping_.number])
            )

            createReadOnlyQuery(entityManager, query)
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