package fr.shikkanime.converters.episode

import fr.shikkanime.converters.AbstractConverter
import fr.shikkanime.dtos.AnimeDto
import fr.shikkanime.dtos.EpisodeDto
import fr.shikkanime.entities.Episode
import fr.shikkanime.utils.withUTC
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
            releaseDateTime = from.releaseDateTime.withUTC().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME),
            season = from.season!!,
            number = from.number!!,
            title = from.title,
            url = from.url!!,
            image = from.image!!,
            duration = from.duration,
            description = from.description,
            uncensored = from.image!!.contains("nc/", true),
        )
    }
}