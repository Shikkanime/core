package fr.shikkanime.repositories

import fr.shikkanime.entities.AnimeTag
import fr.shikkanime.entities.AnimeTag_
import fr.shikkanime.entities.Anime_
import fr.shikkanime.entities.Tag_
import java.util.*

class AnimeTagRepository : AbstractRepository<AnimeTag>() {
    suspend fun findAllByAnime(animeUuid: UUID): List<AnimeTag> {
        return dispatch {
            val cb = it.criteriaBuilder
            val query = cb.createQuery(entityClass)
            val root = query.from(entityClass)

            query.where(cb.equal(root[AnimeTag_.anime][Anime_.uuid], animeUuid))
                .orderBy(cb.asc(cb.lower(root[AnimeTag_.tag][Tag_.name])))

            createReadOnlyQuery(it, query)
                .resultList
        }
    }
}