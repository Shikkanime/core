package fr.shikkanime.services.caches

import com.google.inject.Inject
import fr.shikkanime.entities.Anime
import fr.shikkanime.services.AnimePlatformService
import fr.shikkanime.utils.MapCache
import fr.shikkanime.utils.TelemetryConfig
import fr.shikkanime.utils.TelemetryConfig.span
import java.util.*

class AnimePlatformCacheService : AbstractCacheService {
    private val tracer = TelemetryConfig.getTracer("AnimePlatformCacheService")

    @Inject
    private lateinit var animePlatformService: AnimePlatformService

    fun findAllByAnimeUUID(animeUUID: UUID) = MapCache.getOrCompute(
        "AnimePlatformCacheService.findAllByAnimeUUID",
        classes = listOf(Anime::class.java),
        key = animeUUID,
    ) { tracer.span { animePlatformService.findAllByAnimeUUID(it) } }
}