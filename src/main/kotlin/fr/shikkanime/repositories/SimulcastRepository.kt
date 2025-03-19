package fr.shikkanime.repositories

import fr.shikkanime.dtos.simulcasts.SimulcastModifiedDto
import fr.shikkanime.entities.Anime_
import fr.shikkanime.entities.Simulcast
import fr.shikkanime.entities.Simulcast_
import fr.shikkanime.utils.TelemetryConfig
import fr.shikkanime.utils.TelemetryConfig.span

class SimulcastRepository : AbstractRepository<Simulcast>() {
    private val tracer = TelemetryConfig.getTracer("SimulcastRepository")

    override fun getEntityClass() = Simulcast::class.java

    fun findAllModified(): List<SimulcastModifiedDto> {
        return tracer.span {
            database.entityManager.use {
                val cb = it.criteriaBuilder
                val query = cb.createQuery(SimulcastModifiedDto::class.java)

                val root = query.from(getEntityClass())
                val animeJoin = root.join(Simulcast_.animes)

                query.select(
                    cb.construct(
                        SimulcastModifiedDto::class.java,
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
    }

    fun findBySeasonAndYear(season: String, year: Int): Simulcast? {
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