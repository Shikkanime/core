package fr.shikkanime.services.caches

import com.google.inject.Inject
import fr.shikkanime.entities.Anime
import fr.shikkanime.services.AnimePlatformService
import fr.shikkanime.utils.MapCache

class AnimePlatformCacheService : AbstractCacheService {
    companion object {
        private const val DEFAULT_ALL_KEY = "all"
    }

    @Inject
    private lateinit var animePlatformService: AnimePlatformService

    private val cache = MapCache(
        "AnimePlatformCacheService.cache",
        classes = listOf(Anime::class.java),
        fn = { listOf(DEFAULT_ALL_KEY) }
    ) {
        animePlatformService.findAll()
    }

    fun findAllByAnime(anime: Anime) = cache[DEFAULT_ALL_KEY]?.filter { it.anime?.uuid == anime.uuid } ?: emptyList()
}