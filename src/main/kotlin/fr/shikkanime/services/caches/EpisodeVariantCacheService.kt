package fr.shikkanime.services.caches

import com.google.inject.Inject
import fr.shikkanime.entities.Anime
import fr.shikkanime.entities.EpisodeMapping
import fr.shikkanime.entities.EpisodeVariant
import fr.shikkanime.services.EpisodeVariantService
import fr.shikkanime.utils.MapCache
import java.time.ZonedDateTime

class EpisodeVariantCacheService : AbstractCacheService {
    @Inject
    private lateinit var episodeVariantService: EpisodeVariantService

    private val findAudioLocalesAndSeasonsByAnimeCache =
        MapCache<Anime, Pair<List<String>, List<Pair<Int, ZonedDateTime>>>>(
            log = false,
            classes = listOf(
                EpisodeMapping::class.java,
                EpisodeVariant::class.java
            )
        ) {
            episodeVariantService.findAudioLocalesAndSeasonsByAnime(it)
        }

    fun findAudioLocalesAndSeasonsByAnimeCache(anime: Anime) = findAudioLocalesAndSeasonsByAnimeCache[anime]
}