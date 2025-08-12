package fr.shikkanime.services.caches

import com.google.inject.Inject
import fr.shikkanime.entities.Anime
import fr.shikkanime.services.AnimePlatformService
import fr.shikkanime.utils.MapCache
import fr.shikkanime.utils.StringUtils
import fr.shikkanime.utils.TelemetryConfig
import fr.shikkanime.utils.TelemetryConfig.trace

class AnimePlatformCacheService : ICacheService {
    private val tracer = TelemetryConfig.getTracer("AnimePlatformCacheService")
    @Inject private lateinit var animePlatformService: AnimePlatformService

    fun getAll(anime: Anime) = MapCache.getOrCompute(
        "AnimePlatformCacheService.getAll",
        classes = listOf(Anime::class.java),
        key = StringUtils.EMPTY_STRING,
    ) { tracer.trace { animePlatformService.findAll().groupBy { it.anime!!.uuid } } }[anime.uuid] ?: emptyList()
}