package fr.shikkanime.converters.anime

import fr.shikkanime.converters.AbstractConverter
import fr.shikkanime.dtos.AnimeDto
import fr.shikkanime.dtos.SimulcastDto
import fr.shikkanime.entities.Anime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class AnimeToAnimeDtoConverter : AbstractConverter<Anime, AnimeDto>() {
    private val utcZone = ZoneId.of("UTC")

    override fun convert(from: Anime): AnimeDto {
        return AnimeDto(
            uuid = from.uuid,
            releaseDateTime = from.releaseDateTime.withZoneSameInstant(utcZone).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME),
            image = from.image,
            countryCode = from.countryCode!!,
            name = from.name!!,
            description = from.description,
            simulcasts = convert(from.simulcasts, SimulcastDto::class.java),
        )
    }
}