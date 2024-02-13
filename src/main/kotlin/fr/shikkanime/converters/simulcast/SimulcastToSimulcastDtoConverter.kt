package fr.shikkanime.converters.simulcast

import fr.shikkanime.converters.AbstractConverter
import fr.shikkanime.dtos.simulcasts.SimulcastDto
import fr.shikkanime.entities.Simulcast

class SimulcastToSimulcastDtoConverter : AbstractConverter<Simulcast, SimulcastDto>() {
    override fun convert(from: Simulcast): SimulcastDto {
        return SimulcastDto(
            uuid = from.uuid,
            season = from.season!!,
            year = from.year!!,
            slug = "${from.season.lowercase()}-${from.year}",
            label = when (from.season) {
                "WINTER" -> "Hiver"
                "SPRING" -> "Printemps"
                "SUMMER" -> "Été"
                "AUTUMN" -> "Automne"
                else -> "Inconnu"
            } + " ${from.year}",
        )
    }
}