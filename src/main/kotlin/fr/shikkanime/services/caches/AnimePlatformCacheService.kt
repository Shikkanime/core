package fr.shikkanime.services.caches

import com.google.inject.Inject
import fr.shikkanime.entities.Anime
import fr.shikkanime.services.AnimePlatformService
import fr.shikkanime.utils.MapCache

class AnimePlatformCacheService : AbstractCacheService {
    @Inject
    private lateinit var animePlatformService: AnimePlatformService

    fun getAll(anime: Anime) = MapCache.getOrCompute(
        "AnimePlatformCacheService.getAll",
        classes = listOf(Anime::class.java),
        key = "all",
    ) { animePlatformService.findAll() }
        .filter { it.anime!!.uuid == anime.uuid }
}