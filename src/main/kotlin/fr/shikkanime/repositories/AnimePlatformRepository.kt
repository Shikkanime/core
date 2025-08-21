package fr.shikkanime.repositories

import fr.shikkanime.entities.Anime
import fr.shikkanime.entities.AnimePlatform
import fr.shikkanime.entities.AnimePlatform_
import fr.shikkanime.entities.Anime_
import fr.shikkanime.entities.enums.Platform
import java.util.*

class AnimePlatformRepository : AbstractRepository<AnimePlatform>() {
    override fun getEntityClass() = AnimePlatform::class.java

    fun findAllByAnime(uuid: UUID): List<AnimePlatform> {
        return database.entityManager.use {
            val cb = it.criteriaBuilder
            val query = cb.createQuery(getEntityClass())
            val root = query.from(getEntityClass())

            query.where(cb.equal(root[AnimePlatform_.anime][Anime_.uuid], uuid))

            createReadOnlyQuery(it, query)
                .resultList
        }
    }

    fun findAllIdByAnimeAndPlatform(anime: Anime, platform: Platform): List<String> {
        return database.entityManager.use {
            val cb = it.criteriaBuilder
            val query = cb.createQuery(String::class.java)
            val root = query.from(getEntityClass())
            query.select(root[AnimePlatform_.platformId])

            query.where(
                cb.and(
                    cb.equal(root[AnimePlatform_.anime], anime),
                    cb.equal(root[AnimePlatform_.platform], platform)
                )
            )

            createReadOnlyQuery(it, query)
                .resultList
        }
    }

    fun findByAnimePlatformAndId(anime: Anime, platform: Platform, platformId: String): AnimePlatform? {
        return database.entityManager.use {
            val cb = it.criteriaBuilder
            val query = cb.createQuery(getEntityClass())
            val root = query.from(getEntityClass())

            query.where(
                cb.and(
                    cb.equal(root[AnimePlatform_.anime], anime),
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