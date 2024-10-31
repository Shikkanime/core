package fr.shikkanime.converters.episode_mapping

import com.google.inject.Inject
import fr.shikkanime.converters.AbstractConverter
import fr.shikkanime.dtos.PlatformDto
import fr.shikkanime.dtos.mappings.EpisodeMappingWithoutAnimeDto
import fr.shikkanime.dtos.variants.EpisodeVariantWithoutMappingDto
import fr.shikkanime.entities.EpisodeMapping
import fr.shikkanime.entities.enums.LangType
import fr.shikkanime.services.caches.EpisodeVariantCacheService
import fr.shikkanime.utils.withUTCString

class EpisodeMappingToEpisodeMappingWithoutAnimeDtoConverter :
    AbstractConverter<EpisodeMapping, EpisodeMappingWithoutAnimeDto>() {
    @Inject
    private lateinit var episodeVariantCacheService: EpisodeVariantCacheService

    @Converter
    fun convert(from: EpisodeMapping): EpisodeMappingWithoutAnimeDto {
        val variants = episodeVariantCacheService.findAllByMapping(from) ?: emptyList()

        return EpisodeMappingWithoutAnimeDto(
            uuid = from.uuid!!,
            releaseDateTime = from.releaseDateTime.withUTCString(),
            lastReleaseDateTime = from.lastReleaseDateTime.withUTCString(),
            lastUpdateDateTime = from.lastUpdateDateTime.withUTCString(),
            episodeType = from.episodeType!!,
            season = from.season!!,
            number = from.number!!,
            duration = from.duration,
            title = from.title,
            description = from.description,
            image = from.image!!,
            variants = convert(variants, EpisodeVariantWithoutMappingDto::class.java),
            platforms = convert(
                variants.mapNotNull { it.platform }.sortedBy { it.name }.toSet(),
                PlatformDto::class.java
            )?.toList(),
            langTypes = variants.map { LangType.fromAudioLocale(from.anime!!.countryCode!!, it.audioLocale!!) }
                .distinct()
                .sorted(),
            status = from.status
        )
    }
}