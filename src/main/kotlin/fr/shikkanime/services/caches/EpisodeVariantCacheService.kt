package fr.shikkanime.services.caches

import com.google.inject.Inject
import fr.shikkanime.entities.Anime
import fr.shikkanime.entities.EpisodeMapping
import fr.shikkanime.entities.EpisodeVariant
import fr.shikkanime.services.EpisodeVariantService
import fr.shikkanime.utils.MapCache

class EpisodeVariantCacheService : AbstractCacheService {
    @Inject
    private lateinit var episodeVariantService: EpisodeVariantService

    private val findAllAudioLocalesCache =
        MapCache<Anime, List<String>>(
            log = false,
            classes = listOf(
                EpisodeMapping::class.java,
                EpisodeVariant::class.java
            )
        ) {
            episodeVariantService.findAllAudioLocalesByAnime(it)
        }

    fun findAllAudioLocalesByAnime(anime: Anime) = findAllAudioLocalesCache[anime]
}