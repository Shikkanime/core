package fr.shikkanime.repositories

import fr.shikkanime.entities.AnimeTag
import fr.shikkanime.entities.AnimeTag_
import fr.shikkanime.entities.Anime_
import fr.shikkanime.entities.Tag_
import java.util.*

class AnimeTagRepository : AbstractRepository<AnimeTag>() {
    override fun getEntityClass() = AnimeTag::class.java

    fun findAllByAnime(animeUuid: UUID): List<AnimeTag> {
        return database.entityManager.use {
            val cb = it.criteriaBuilder
            val query = cb.createQuery(getEntityClass())
            val root = query.from(getEntityClass())

            query.where(cb.equal(root[AnimeTag_.anime][Anime_.uuid], animeUuid))
                .orderBy(cb.asc(cb.lower(root[AnimeTag_.tag][Tag_.name])))

            createReadOnlyQuery(it, query)
                .resultList
        }
    }
}