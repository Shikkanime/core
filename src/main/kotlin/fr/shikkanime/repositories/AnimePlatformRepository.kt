package fr.shikkanime.repositories

import fr.shikkanime.entities.AnimePlatform
import fr.shikkanime.entities.AnimePlatform_
import fr.shikkanime.entities.Anime_
import fr.shikkanime.entities.enums.Platform
import java.util.*

class AnimePlatformRepository : AbstractRepository<AnimePlatform>() {
    suspend fun findAllByAnime(uuid: UUID): List<AnimePlatform> {
        return dispatch {
            val cb = it.criteriaBuilder
            val query = cb.createQuery(getEntityClass())
            val root = query.from(getEntityClass())

            query.where(cb.equal(root[AnimePlatform_.anime][Anime_.uuid], uuid))

            createReadOnlyQuery(it, query)
                .resultList
        }
    }

    suspend fun findAllByPlatform(platform: Platform): List<AnimePlatform> {
        return dispatch {
            val cb = it.criteriaBuilder
            val query = cb.createQuery(getEntityClass())
            val root = query.from(getEntityClass())

            query.where(cb.equal(root[AnimePlatform_.platform], platform))

            createReadOnlyQuery(it, query)
                .resultList
        }
    }

    suspend fun findAllIdByAnimeAndPlatform(animeUuid: UUID, platform: Platform): List<String> {
        return dispatch {
            val cb = it.criteriaBuilder
            val query = cb.createQuery(String::class.java)
            val root = query.from(getEntityClass())
            query.select(root[AnimePlatform_.platformId])

            query.where(
                cb.and(
                    cb.equal(root[AnimePlatform_.anime][Anime_.uuid], animeUuid),
                    cb.equal(root[AnimePlatform_.platform], platform)
                )
            )

            createReadOnlyQuery(it, query)
                .resultList
        }
    }

    suspend fun findByAnimePlatformAndId(animeUuid: UUID, platform: Platform, platformId: String): AnimePlatform? {
        return dispatch {
            val cb = it.criteriaBuilder
            val query = cb.createQuery(getEntityClass())
            val root = query.from(getEntityClass())

            query.where(
                cb.and(
                    cb.equal(root[AnimePlatform_.anime][Anime_.uuid], animeUuid),
                    cb.equal(root[AnimePlatform_.platform], platform),
                    cb.equal(root[AnimePlatform_.platformId], platformId)
                )
            )

            createReadOnlyQuery(it, query)
                .resultList
                .firstOrNull()
        }
    }
}