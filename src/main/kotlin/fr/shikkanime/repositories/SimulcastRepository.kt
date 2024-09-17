package fr.shikkanime.repositories

import fr.shikkanime.entities.Anime
import fr.shikkanime.entities.Anime_
import fr.shikkanime.entities.Simulcast
import fr.shikkanime.entities.Simulcast_
import jakarta.persistence.Tuple

class SimulcastRepository : AbstractRepository<Simulcast>() {
    override fun getEntityClass() = Simulcast::class.java

    override fun findAll(): List<Simulcast> {
        return database.entityManager.use {
            val cb = it.criteriaBuilder
            val query = cb.createQuery(getEntityClass())
            val root = query.from(getEntityClass())

            // Query where the animes are not empty
            query.where(cb.isNotEmpty(root[Simulcast_.animes]))

            createReadOnlyQuery(it, query)
                .resultList
        }
    }

    fun findAllModified(): List<Tuple> {
        return database.entityManager.use {
            val cb = it.criteriaBuilder
            val query = cb.createTupleQuery()

            val root = query.from(Anime::class.java)
            val simulcastJoin = root.join(Anime_.simulcasts)

            query.multiselect(
                simulcastJoin,
                cb.greatest(root[Anime_.releaseDateTime])
            )

            query.groupBy(simulcastJoin)
            query.orderBy(cb.desc(cb.greatest(root[Anime_.releaseDateTime])))

            createReadOnlyQuery(it, query)
                .resultList
        }
    }


    fun findBySeasonAndYear(season: String, year: Int): Simulcast? {
        return database.entityManager.use {
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