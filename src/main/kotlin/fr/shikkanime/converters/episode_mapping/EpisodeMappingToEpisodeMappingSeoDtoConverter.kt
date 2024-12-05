package fr.shikkanime.converters.episode_mapping

import fr.shikkanime.converters.AbstractConverter
import fr.shikkanime.dtos.mappings.EpisodeMappingSeoDto
import fr.shikkanime.entities.EpisodeMapping

class EpisodeMappingToEpisodeMappingSeoDtoConverter : AbstractConverter<EpisodeMapping, EpisodeMappingSeoDto>() {
    @Converter
    fun convert(from: EpisodeMapping): EpisodeMappingSeoDto {
        return EpisodeMappingSeoDto(
            animeSlug = from.anime!!.slug!!,
            season = from.season!!,
            episodeType = from.episodeType!!,
            number = from.number!!,
            lastReleaseDateTime = from.lastReleaseDateTime,
        )
    }
}