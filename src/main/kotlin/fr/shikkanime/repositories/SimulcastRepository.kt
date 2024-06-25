package fr.shikkanime.repositories

import fr.shikkanime.entities.Simulcast
import fr.shikkanime.entities.Simulcast_

class SimulcastRepository : AbstractRepository<Simulcast>() {
    override fun getEntityClass() = Simulcast::class.java

    override fun findAll(): List<Simulcast> {
        val cb = entityManager.criteriaBuilder
        val query = cb.createQuery(getEntityClass())
        val root = query.from(getEntityClass())

        // Query where the animes are not empty
        query.where(cb.isNotEmpty(root[Simulcast_.animes]))

        return createReadOnlyQuery(entityManager, query)
            .resultList
    }

    fun findBySeasonAndYear(season: String, year: Int): Simulcast? {
        val cb = entityManager.criteriaBuilder
        val query = cb.createQuery(getEntityClass())
        val root = query.from(getEntityClass())

        query.where(
            cb.equal(root[Simulcast_.season], season),
            cb.equal(root[Simulcast_.year], year)
        )

        return createReadOnlyQuery(entityManager, query)
            .resultList
            .firstOrNull()
    }
}