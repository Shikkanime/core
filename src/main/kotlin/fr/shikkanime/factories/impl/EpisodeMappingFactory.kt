package fr.shikkanime.factories.impl

import com.google.inject.Inject
import fr.shikkanime.dtos.mappings.EpisodeMappingDto
import fr.shikkanime.entities.EpisodeMapping
import fr.shikkanime.entities.enums.LangType
import fr.shikkanime.factories.IEpisodeMappingFactory
import fr.shikkanime.services.caches.EpisodeVariantCacheService
import fr.shikkanime.utils.withUTCString

class EpisodeMappingFactory : IEpisodeMappingFactory {
    @Inject private lateinit var episodeVariantCacheService: EpisodeVariantCacheService
    @Inject private lateinit var episodeVariantFactory: EpisodeVariantFactory
    @Inject private lateinit var platformFactory: PlatformFactory
    @Inject private lateinit var animeFactory: AnimeFactory

    override fun toDto(
        entity: EpisodeMapping,
        useAnime: Boolean
    ): EpisodeMappingDto {
        val variants = episodeVariantCacheService.findAllByMapping(entity)

        return EpisodeMappingDto(
            uuid = entity.uuid!!,
            anime = if (useAnime) animeFactory.toDto(entity.anime!!) else null,
            releaseDateTime = entity.releaseDateTime.withUTCString(),
            lastReleaseDateTime = entity.lastReleaseDateTime.withUTCString(),
            lastUpdateDateTime = entity.lastUpdateDateTime.withUTCString(),
            episodeType = entity.episodeType!!,
            season = entity.season!!,
            number = entity.number!!,
            duration = entity.duration,
            title = entity.title,
            description = entity.description,
            variants = variants.map { episodeVariantFactory.toDto(it, false) }.toSet(),
            platforms = variants.asSequence()
                .mapNotNull { it.platform }
                .sortedBy { it.name }
                .map { platformFactory.toDto(it) }
                .toSet(),
            langTypes = variants.map { LangType.fromAudioLocale(entity.anime!!.countryCode!!, it.audioLocale!!) }
                .sorted()
                .toSet(),
        )
    }
}