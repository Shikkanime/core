package fr.shikkanime.repositories

import fr.shikkanime.entities.Anime_
import fr.shikkanime.entities.EpisodeMapping_
import fr.shikkanime.entities.EpisodeVariant
import fr.shikkanime.entities.EpisodeVariant_
import fr.shikkanime.entities.enums.CountryCode
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