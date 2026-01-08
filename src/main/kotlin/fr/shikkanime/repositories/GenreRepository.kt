package fr.shikkanime.repositories

import fr.shikkanime.entities.Anime
import fr.shikkanime.entities.Anime_
import fr.shikkanime.entities.Genre_
import fr.shikkanime.entities.Genre
import java.util.UUID

class GenreRepository : AbstractRepository<Genre>() {
    override fun getEntityClass() = Genre::class.java

    fun findAllByAnime(animeUuid: UUID): List<Genre> {
        return database.entityManager.use {
            val cb = it.criteriaBuilder
            val query = cb.createQuery(getEntityClass())
            val root = query.from(Anime::class.java)
            val genreJoin = root.join(Anime_.genres)

            query.select(genreJoin)
                .where(cb.equal(root[Anime_.uuid], animeUuid))
                .orderBy(cb.asc(cb.lower(genreJoin[Genre_.name])))

            createReadOnlyQuery(it, query)
                .resultList
        }
    }

    fun findByName(name: String): Genre? {
        return database.entityManager.use {
            val cb = it.criteriaBuilder
            val query = cb.createQuery(getEntityClass())
            val root = query.from(getEntityClass())
            query.where(cb.equal(cb.lower(root[Genre_.name]), name.lowercase()))

            createReadOnlyQuery(it, query)
                .resultList
                .firstOrNull()
        }
    }
}