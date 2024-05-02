package fr.shikkanime.repositories

import fr.shikkanime.entities.*
import fr.shikkanime.entities.enums.CountryCode
import fr.shikkanime.entities.enums.EpisodeType
import jakarta.persistence.Tuple
import java.time.ZonedDateTime
import java.util.*

class EpisodeVariantRepository : AbstractRepository<EpisodeVariant>() {
    override fun getEntityClass() = EpisodeVariant::class.java

    fun findAllByDateRange(
        countryCode: CountryCode,
        start: ZonedDateTime,
        end: ZonedDateTime,
    ): List<EpisodeVariant> {
        return inTransaction { entityManager ->
            val cb = entityManager.criteriaBuilder
            val query = cb.createQuery(getEntityClass())
            val root = query.from(getEntityClass())

            val countryPredicate =
                cb.equal(root[EpisodeVariant_.mapping][EpisodeMapping_.anime][Anime_.countryCode], countryCode)
            val datePredicate = cb.between(root[EpisodeVariant_.releaseDateTime], start, end)

            query.where(cb.and(countryPredicate, datePredicate))

            createReadOnlyQuery(entityManager, query)
                .resultList
        }
    }

    fun findAllTypeIdentifier(): List<Tuple> {
        return inTransaction { entityManager ->
            val cb = entityManager.criteriaBuilder
            val query = cb.createTupleQuery()
            val root = query.from(getEntityClass())

            val episodeMappingPath = root[EpisodeVariant_.mapping]
            val animePath = episodeMappingPath[EpisodeMapping_.anime]

            query.multiselect(
                animePath[Anime_.countryCode],
                animePath.get<UUID>(Anime_.UUID),
                root[EpisodeVariant_.platform],
                episodeMappingPath[EpisodeMapping_.episodeType],
                episodeMappingPath[EpisodeMapping_.season],
                episodeMappingPath[EpisodeMapping_.number],
                root[EpisodeVariant_.audioLocale],
                root[EpisodeVariant_.identifier]
            )

            createReadOnlyQuery(entityManager, query)
                .resultList
        }
    }

    fun findAllAudioLocalesByAnime(anime: Anime): List<String> {
        return inTransaction { entityManager ->
            val cb = entityManager.criteriaBuilder
            val query = cb.createQuery(String::class.java)
            val root = query.from(getEntityClass())

            query.select(root[EpisodeVariant_.audioLocale])
                .where(cb.equal(root[EpisodeVariant_.mapping][EpisodeMapping_.anime], anime))
                .distinct(true)

            createReadOnlyQuery(entityManager, query)
                .resultList
        }
    }

    fun findAllAudioLocalesByMapping(mapping: EpisodeMapping): List<String> {
        return inTransaction { entityManager ->
            val cb = entityManager.criteriaBuilder
            val query = cb.createQuery(String::class.java)
            val root = query.from(getEntityClass())

            query.select(root[EpisodeVariant_.audioLocale])
                .where(cb.equal(root[EpisodeVariant_.mapping], mapping))
                .distinct(true)

            createReadOnlyQuery(entityManager, query)
                .resultList
        }
    }

    fun findAllByAnime(anime: Anime): List<EpisodeVariant> {
        return inTransaction { entityManager ->
            val cb = entityManager.criteriaBuilder
            val query = cb.createQuery(getEntityClass())
            val root = query.from(getEntityClass())

            query.where(cb.equal(root[EpisodeVariant_.mapping][EpisodeMapping_.anime], anime))

            createReadOnlyQuery(entityManager, query)
                .resultList
        }
    }

    fun findAllByMapping(mapping: EpisodeMapping): List<EpisodeVariant> {
        return inTransaction { entityManager ->
            val cb = entityManager.criteriaBuilder
            val query = cb.createQuery(getEntityClass())
            val root = query.from(getEntityClass())

            query.where(cb.equal(root[EpisodeVariant_.mapping], mapping))

            createReadOnlyQuery(entityManager, query)
                .resultList
        }
    }

    fun findAllSimulcasted(countryCode: CountryCode): List<EpisodeMapping> {
        return inTransaction { entityManager ->
            val cb = entityManager.criteriaBuilder
            val query = cb.createQuery(EpisodeMapping::class.java)
            val root = query.from(getEntityClass())

            query.distinct(true)
                .select(root[EpisodeVariant_.mapping])
                .where(
                    cb.and(
                        cb.notEqual(root[EpisodeVariant_.audioLocale], countryCode.locale),
                        cb.notEqual(root[EpisodeVariant_.mapping][EpisodeMapping_.episodeType], EpisodeType.FILM)
                    )
                )
                .orderBy(cb.asc(root[EpisodeVariant_.mapping][EpisodeMapping_.releaseDateTime]))

            createReadOnlyQuery(entityManager, query)
                .resultList
        }
    }

    fun findByIdentifier(identifier: String): EpisodeVariant? {
        return inTransaction { entityManager ->
            val cb = entityManager.criteriaBuilder
            val query = cb.createQuery(getEntityClass())
            val root = query.from(getEntityClass())

            query.where(cb.equal(root[EpisodeVariant_.identifier], identifier))

            createReadOnlyQuery(entityManager, query)
                .resultList
                .firstOrNull()
        }
    }
}