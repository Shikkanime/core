package fr.shikkanime.converters.episode_mapping

import fr.shikkanime.converters.AbstractConverter
import fr.shikkanime.dtos.mappings.EpisodeMappingWithoutAnimeDto
import fr.shikkanime.entities.EpisodeMapping
import fr.shikkanime.utils.withUTCString

class EpisodeMappingToEpisodeMappingWithoutAnimeDtoConverter :
    AbstractConverter<EpisodeMapping, EpisodeMappingWithoutAnimeDto>() {
    override fun convert(from: EpisodeMapping): EpisodeMappingWithoutAnimeDto {
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
            status = from.status
        )
    }
}