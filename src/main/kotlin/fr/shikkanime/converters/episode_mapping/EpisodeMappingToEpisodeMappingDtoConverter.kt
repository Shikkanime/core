package fr.shikkanime.converters.episode_mapping

import com.google.inject.Inject
import fr.shikkanime.converters.AbstractConverter
import fr.shikkanime.dtos.AnimeDto
import fr.shikkanime.dtos.EpisodeMappingDto
import fr.shikkanime.dtos.PlatformDto
import fr.shikkanime.dtos.variants.EpisodeVariantWithoutMappingDto
import fr.shikkanime.entities.EpisodeMapping
import fr.shikkanime.entities.enums.LangType
import fr.shikkanime.services.EpisodeVariantService
import fr.shikkanime.utils.withUTCString

class EpisodeMappingToEpisodeMappingDtoConverter : AbstractConverter<EpisodeMapping, EpisodeMappingDto>() {
    @Inject
    private lateinit var episodeVariantService: EpisodeVariantService

    override fun convert(from: EpisodeMapping): EpisodeMappingDto {
        val variants = episodeVariantService.findAllByMapping(from).sortedBy { it.releaseDateTime }

        return EpisodeMappingDto(
            uuid = from.uuid!!,
            anime = convert(from.anime, AnimeDto::class.java),
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