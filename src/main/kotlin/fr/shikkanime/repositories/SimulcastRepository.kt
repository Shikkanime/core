package fr.shikkanime.repositories

import fr.shikkanime.entities.Anime_
import fr.shikkanime.entities.Simulcast
import fr.shikkanime.entities.Simulcast_
import fr.shikkanime.entities.enums.Season
import jakarta.persistence.Tuple

class SimulcastRepository : AbstractRepository<Simulcast>() {
    override fun getEntityClass() = Simulcast::class.java

    fun findAllModified(): List<Tuple> {
        return database.entityManager.use {
            val cb = it.criteriaBuilder
            val query = cb.createTupleQuery()

            val root = query.from(getEntityClass())
            val animeJoin = root.join(Simulcast_.animes)

            query.select(
                cb.tuple(
                    root,
                    cb.greatest(animeJoin[Anime_.releaseDateTime]),
                    cb.count(animeJoin)
                )
            )

            query.groupBy(root)
            query.orderBy(cb.desc(cb.greatest(animeJoin[Anime_.releaseDateTime])))

            createReadOnlyQuery(it, query)
                .resultList
        }
    }

    fun findBySeasonAndYear(season: Season, year: Int): Simulcast? {
        return database.entityManager.use {
            val cb = it.criteriaBuilder
            val query = cb.createQuery(getEntityClass())
            val root = query.from(getEntityClass())

            query.select(root)
                .where(
                    cb.and(
                        cb.equal(root[Simulcast_.season], season),
                        cb.equal(root[Simulcast_.year], year)
                    )
                )


            createReadOnlyQuery(it, query)
                .setMaxResults(1)
                .resultList
                .firstOrNull()
        }
    }
}