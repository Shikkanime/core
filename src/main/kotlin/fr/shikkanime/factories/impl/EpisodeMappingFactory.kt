package fr.shikkanime.factories.impl

import com.google.inject.Inject
import fr.shikkanime.dtos.mappings.EpisodeMappingDto
import fr.shikkanime.entities.EpisodeMapping
import fr.shikkanime.factories.IEpisodeMappingFactory
import fr.shikkanime.services.caches.EpisodeVariantCacheService
import fr.shikkanime.utils.toTreeSet
import fr.shikkanime.utils.withUTCString

class EpisodeMappingFactory : IEpisodeMappingFactory {
    @Inject private lateinit var episodeVariantCacheService: EpisodeVariantCacheService
    @Inject private lateinit var animeFactory: AnimeFactory
    @Inject private lateinit var episodeSourceFactory: EpisodeSourceFactory

    override fun toDto(
        entity: EpisodeMapping,
        useAnime: Boolean
    ): EpisodeMappingDto {
        val variants = episodeVariantCacheService.findAllByMapping(entity)
        val countryCode = entity.anime!!.countryCode!!

        return EpisodeMappingDto(
            uuid = entity.uuid!!,
            anime = animeFactory.takeIf { useAnime }?.toDto(entity.anime!!),
            releaseDateTime = entity.releaseDateTime.withUTCString(),
            lastReleaseDateTime = entity.lastReleaseDateTime.withUTCString(),
            lastUpdateDateTime = entity.lastUpdateDateTime.withUTCString(),
            episodeType = entity.episodeType!!,
            season = entity.season!!,
            number = entity.number!!,
            duration = entity.duration,
            title = entity.title,
            description = entity.description,
            variants = variants.toSet(),
            sources = variants.map { episodeSourceFactory.toDto(countryCode, it) }.toTreeSet(),
        )
    }
}