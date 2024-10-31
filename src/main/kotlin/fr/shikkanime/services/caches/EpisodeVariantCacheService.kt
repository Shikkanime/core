package fr.shikkanime.services.caches

import com.google.inject.Inject
import fr.shikkanime.entities.EpisodeMapping
import fr.shikkanime.entities.EpisodeVariant
import fr.shikkanime.services.EpisodeVariantService
import fr.shikkanime.utils.MapCache
import java.util.UUID
import kotlin.collections.get

class EpisodeVariantCacheService : AbstractCacheService {
    @Inject
    private lateinit var episodeVariantService: EpisodeVariantService

    private val findAllCache = MapCache<String, Map<UUID, List<EpisodeVariant>>>(
        classes = listOf(EpisodeMapping::class.java, EpisodeVariant::class.java),
    ) {
        episodeVariantService.findAll().sortedBy { it.releaseDateTime }
            .groupBy { it.mapping!!.uuid!! }
    }

    fun findAllByMapping(episodeMapping: EpisodeMapping) =
        findAllCache["all"]?.get(episodeMapping.uuid)
}