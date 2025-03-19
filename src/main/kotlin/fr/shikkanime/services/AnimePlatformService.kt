package fr.shikkanime.services

import com.google.inject.Inject
import fr.shikkanime.entities.Anime
import fr.shikkanime.entities.AnimePlatform
import fr.shikkanime.entities.enums.Platform
import fr.shikkanime.repositories.AnimePlatformRepository
import fr.shikkanime.utils.TelemetryConfig
import fr.shikkanime.utils.TelemetryConfig.span
import java.util.*

class AnimePlatformService : AbstractService<AnimePlatform, AnimePlatformRepository>() {
    private val tracer = TelemetryConfig.getTracer("AnimePlatformService")

    @Inject
    private lateinit var animePlatformRepository: AnimePlatformRepository

    override fun getRepository() = animePlatformRepository

    fun findAllByAnimeUUID(animeUUID: UUID) = tracer.span { animePlatformRepository.findAllByAnimeUUID(animeUUID) }

    fun findAllByAnime(anime: Anime) = findAllByAnimeUUID(anime.uuid!!)

    fun findByAnimePlatformAndId(anime: Anime, platform: Platform, platformId: String) =
        animePlatformRepository.findByAnimePlatformAndId(anime, platform, platformId)
}