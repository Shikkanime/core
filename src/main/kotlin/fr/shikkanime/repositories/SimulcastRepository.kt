package fr.shikkanime.repositories

import fr.shikkanime.entities.Simulcast
import fr.shikkanime.entities.Simulcast_

class SimulcastRepository : AbstractRepository<Simulcast>() {
    override fun getEntityClass() = Simulcast::class.java

    fun findBySeasonAndYear(season: String, year: Int): Simulcast? {
        return inTransaction {
            val cb = it.criteriaBuilder
            val query = cb.createQuery(getEntityClass())
            val root = query.from(getEntityClass())

            query.where(
                cb.equal(root[Simulcast_.season], season),
                cb.equal(root[Simulcast_.year], year)
            )

            createReadOnlyQuery(it, query)
                .resultList
                .firstOrNull()
        }
    }
}