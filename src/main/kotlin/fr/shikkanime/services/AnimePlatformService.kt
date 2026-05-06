package fr.shikkanime.services

import fr.shikkanime.entities.Anime
import fr.shikkanime.entities.AnimePlatform
import fr.shikkanime.entities.enums.Platform
import fr.shikkanime.repositories.AnimePlatformRepository
import java.util.*

class AnimePlatformService : AbstractService<AnimePlatform, AnimePlatformRepository>() {
    suspend fun findAllByAnime(uuid: UUID) = repository.findAllByAnime(uuid)

    suspend fun findAllByAnime(anime: Anime) = repository.findAllByAnime(anime.uuid!!)

    suspend fun findAllByPlatform(platform: Platform) = repository.findAllByPlatform(platform)

    suspend fun findAllIdByAnimeAndPlatform(animeUuid: UUID, platform: Platform) =
        repository.findAllIdByAnimeAndPlatform(animeUuid, platform)

    suspend fun findAllIdByAnimeAndPlatform(anime: Anime, platform: Platform) =
        findAllIdByAnimeAndPlatform(anime.uuid!!, platform)

    suspend fun findByAnimePlatformAndId(animeUuid: UUID, platform: Platform, platformId: String) =
        repository.findByAnimePlatformAndId(animeUuid, platform, platformId)

    suspend fun findByAnimePlatformAndId(anime: Anime, platform: Platform, platformId: String) =
        findByAnimePlatformAndId(anime.uuid!!, platform, platformId)
}