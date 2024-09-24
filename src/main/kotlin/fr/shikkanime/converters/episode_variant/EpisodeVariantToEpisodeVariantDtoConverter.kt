package fr.shikkanime.converters.episode_variant

import fr.shikkanime.converters.AbstractConverter
import fr.shikkanime.dtos.PlatformDto
import fr.shikkanime.dtos.mappings.EpisodeMappingDto
import fr.shikkanime.dtos.variants.EpisodeVariantDto
import fr.shikkanime.entities.EpisodeVariant
import fr.shikkanime.utils.withUTCString

class EpisodeVariantToEpisodeVariantDtoConverter : AbstractConverter<EpisodeVariant, EpisodeVariantDto>() {
    @Converter
    fun convert(from: EpisodeVariant): EpisodeVariantDto {
        return EpisodeVariantDto(
            uuid = from.uuid!!,
            mapping = convert(from.mapping, EpisodeMappingDto::class.java),
            releaseDateTime = from.releaseDateTime.withUTCString(),
            platform = convert(from.platform, PlatformDto::class.java),
            audioLocale = from.audioLocale!!,
            identifier = from.identifier!!,
            url = from.url!!,
            uncensored = from.uncensored
        )
    }
}