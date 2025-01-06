package fr.shikkanime.repositories

import fr.shikkanime.entities.EpisodeMapping
import fr.shikkanime.entities.EpisodeMapping_
import fr.shikkanime.entities.EpisodeVariant
import fr.shikkanime.entities.EpisodeVariant_
import jakarta.persistence.criteria.JoinType

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