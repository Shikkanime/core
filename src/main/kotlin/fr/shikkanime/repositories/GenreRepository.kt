package fr.shikkanime.repositories

import fr.shikkanime.entities.Anime
import fr.shikkanime.entities.Anime_
import fr.shikkanime.entities.Genre
import fr.shikkanime.entities.Genre_
import java.util.*

class GenreRepository : AbstractRepository<Genre>() {
    suspend fun findAllByAnime(animeUuid: UUID): List<Genre> {
        return dispatch {
            val cb = it.criteriaBuilder
            val query = cb.createQuery(entityClass)
            val root = query.from(Anime::class.java)
            val genreJoin = root.join(Anime_.genres)

            query.select(genreJoin)
                .where(cb.equal(root[Anime_.uuid], animeUuid))
                .orderBy(cb.asc(cb.lower(genreJoin[Genre_.name])))

            createReadOnlyQuery(it, query)
                .resultList
        }
    }

    suspend fun findByName(name: String): Genre? {
        return dispatch {
            val cb = it.criteriaBuilder
            val query = cb.createQuery(entityClass)
            val root = query.from(entityClass)
            query.where(cb.equal(cb.lower(root[Genre_.name]), name.lowercase()))

            createReadOnlyQuery(it, query)
                .resultList
                .firstOrNull()
        }
    }
}