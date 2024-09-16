package fr.shikkanime.repositories

import fr.shikkanime.entities.Anime
import fr.shikkanime.entities.AnimePlatform
import fr.shikkanime.entities.AnimePlatform_
import fr.shikkanime.entities.enums.Platform

class AnimePlatformRepository : AbstractRepository<AnimePlatform>() {
    override fun getEntityClass() = AnimePlatform::class.java

    fun findAllByAnime(anime: Anime): List<AnimePlatform> {
        return database.entityManager.use {
            val cb = it.criteriaBuilder
            val query = cb.createQuery(getEntityClass())
            val root = query.from(getEntityClass())

            query.where(cb.equal(root[AnimePlatform_.anime], anime))

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