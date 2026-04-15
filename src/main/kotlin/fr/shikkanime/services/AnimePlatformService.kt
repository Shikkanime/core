package fr.shikkanime.services

import fr.shikkanime.entities.Anime
import fr.shikkanime.entities.AnimePlatform
import fr.shikkanime.entities.enums.Platform
import fr.shikkanime.repositories.AnimePlatformRepository
import java.util.*

class AnimePlatformService : AbstractService<AnimePlatform, AnimePlatformRepository>() {
    fun findAllByAnime(uuid: UUID) = repository.findAllByAnime(uuid)

    fun findAllByAnime(anime: Anime) = repository.findAllByAnime(anime.uuid!!)

    fun findAllByPlatform(platform: Platform) = repository.findAllByPlatform(platform)

    fun findAllIdByAnimeAndPlatform(animeUuid: UUID, platform: Platform) =
        repository.findAllIdByAnimeAndPlatform(animeUuid, platform)

    fun findAllIdByAnimeAndPlatform(anime: Anime, platform: Platform) =
        findAllIdByAnimeAndPlatform(anime.uuid!!, platform)

    fun findByAnimePlatformAndId(animeUuid: UUID, platform: Platform, platformId: String) =
        repository.findByAnimePlatformAndId(animeUuid, platform, platformId)

    fun findByAnimePlatformAndId(anime: Anime, platform: Platform, platformId: String) =
        findByAnimePlatformAndId(anime.uuid!!, platform, platformId)
}