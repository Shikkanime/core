package fr.shikkanime.repositories

import fr.shikkanime.entities.*
import fr.shikkanime.entities.enums.CountryCode
import fr.shikkanime.entities.enums.Platform
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