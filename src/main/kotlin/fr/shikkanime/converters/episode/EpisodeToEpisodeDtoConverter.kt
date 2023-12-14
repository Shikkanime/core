package fr.shikkanime.converters.episode

import fr.shikkanime.converters.AbstractConverter
import fr.shikkanime.dtos.AnimeDto
import fr.shikkanime.dtos.EpisodeDto
import fr.shikkanime.entities.Episode
import java.time.format.DateTimeFormatter

class EpisodeToEpisodeDtoConverter : AbstractConverter<Episode, EpisodeDto>() {
    override fun convert(from: Episode): EpisodeDto {
        return EpisodeDto(
            uuid = from.uuid,
            platform = from.platform!!,
            anime = convert(from.anime, AnimeDto::class.java),
            episodeType = from.episodeType!!,
            langType = from.langType!!,
            hash = from.hash!!,
            releaseDateTime = from.releaseDateTime.format(DateTimeFormatter.ISO_DATE),
            season = from.season!!,
            number = from.number!!,
            title = from.title,
            url = from.url!!,
            image = from.image!!,
            duration = from.duration,
        )
    }
}