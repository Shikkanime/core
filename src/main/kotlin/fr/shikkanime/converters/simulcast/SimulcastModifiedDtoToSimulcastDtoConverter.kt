package fr.shikkanime.converters.simulcast

import fr.shikkanime.converters.AbstractConverter
import fr.shikkanime.dtos.simulcasts.SimulcastDto
import fr.shikkanime.dtos.simulcasts.SimulcastModifiedDto
import fr.shikkanime.utils.withUTCString

class SimulcastModifiedDtoToSimulcastDtoConverter : AbstractConverter<SimulcastModifiedDto, SimulcastDto>() {
    @Converter
    fun convert(from: SimulcastModifiedDto): SimulcastDto {
        return SimulcastDto(
            uuid = from.simulcast.uuid,
            season = from.simulcast.season!!,
            year = from.simulcast.year!!,
            slug = "${from.simulcast.season.lowercase()}-${from.simulcast.year}",
            label = when (from.simulcast.season) {
                "WINTER" -> "Hiver"
                "SPRING" -> "Printemps"
                "SUMMER" -> "Été"
                "AUTUMN" -> "Automne"
                else -> "Inconnu"
            } + " ${from.simulcast.year}",
            lastReleaseDateTime = from.lastReleaseDateTime.withUTCString()
        )
    }
}