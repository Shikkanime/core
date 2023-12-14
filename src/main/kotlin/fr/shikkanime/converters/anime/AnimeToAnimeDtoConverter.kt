package fr.shikkanime.converters.anime

import fr.shikkanime.converters.AbstractConverter
import fr.shikkanime.dtos.AnimeDto
import fr.shikkanime.dtos.SimulcastDto
import fr.shikkanime.entities.Anime
import java.time.format.DateTimeFormatter

class AnimeToAnimeDtoConverter : AbstractConverter<Anime, AnimeDto>() {
    override fun convert(from: Anime): AnimeDto {
        return AnimeDto(
            uuid = from.uuid,
            releaseDateTime = from.releaseDateTime.format(DateTimeFormatter.ISO_DATE_TIME),
            countryCode = from.countryCode!!,
            name = from.name!!,
            description = from.description,
            simulcasts = convert(from.simulcasts, SimulcastDto::class.java)
        )
    }
}