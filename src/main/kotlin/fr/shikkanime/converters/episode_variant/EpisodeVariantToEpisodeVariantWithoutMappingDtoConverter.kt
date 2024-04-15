package fr.shikkanime.converters.episode_variant

import fr.shikkanime.converters.AbstractConverter
import fr.shikkanime.dtos.PlatformDto
import fr.shikkanime.dtos.variants.EpisodeVariantWithoutMappingDto
import fr.shikkanime.entities.EpisodeVariant
import fr.shikkanime.utils.withUTCString

class EpisodeVariantToEpisodeVariantWithoutMappingDtoConverter : AbstractConverter<EpisodeVariant, EpisodeVariantWithoutMappingDto>() {
    override fun convert(from: EpisodeVariant): EpisodeVariantWithoutMappingDto {
        return EpisodeVariantWithoutMappingDto(
            uuid = from.uuid!!,
            releaseDateTime = from.releaseDateTime.withUTCString(),
            platform = convert(from.platform, PlatformDto::class.java),
            audioLocale = from.audioLocale!!,
            identifier = from.identifier!!,
            url = from.url!!,
            uncensored = from.uncensored
        )
    }
}