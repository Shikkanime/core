package fr.shikkanime.services

import com.google.inject.Inject
import fr.shikkanime.entities.Anime
import fr.shikkanime.entities.AnimePlatform
import fr.shikkanime.entities.enums.Platform
import fr.shikkanime.repositories.AnimePlatformRepository

class AnimePlatformService : AbstractService<AnimePlatform, AnimePlatformRepository>() {
    @Inject private lateinit var animePlatformRepository: AnimePlatformRepository

    override fun getRepository() = animePlatformRepository

    fun findAllByAnime(anime: Anime) = animePlatformRepository.findAllByAnime(anime)

    fun findAllIdByAnimeAndPlatform(anime: Anime, platform: Platform) =
        animePlatformRepository.findAllIdByAnimeAndPlatform(anime, platform)

    fun findByAnimePlatformAndId(anime: Anime, platform: Platform, platformId: String) =
        animePlatformRepository.findByAnimePlatformAndId(anime, platform, platformId)
}