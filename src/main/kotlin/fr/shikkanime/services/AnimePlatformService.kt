package fr.shikkanime.services

import com.google.inject.Inject
import fr.shikkanime.entities.Anime
import fr.shikkanime.entities.AnimePlatform
import fr.shikkanime.entities.enums.Platform
import fr.shikkanime.repositories.AnimePlatformRepository
import java.util.*

class AnimePlatformService : AbstractService<AnimePlatform, AnimePlatformRepository>() {
    @Inject private lateinit var animePlatformRepository: AnimePlatformRepository

    override fun getRepository() = animePlatformRepository

    fun findAllByAnime(uuid: UUID) = animePlatformRepository.findAllByAnime(uuid)

    fun findAllByAnime(anime: Anime) = animePlatformRepository.findAllByAnime(anime.uuid!!)

    fun findAllIdByAnimeAndPlatform(anime: Anime, platform: Platform) =
        animePlatformRepository.findAllIdByAnimeAndPlatform(anime, platform)

    fun findByAnimePlatformAndId(anime: Anime, platform: Platform, platformId: String) =
        animePlatformRepository.findByAnimePlatformAndId(anime, platform, platformId)
}