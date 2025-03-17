package fr.shikkanime.factories.impl

import fr.shikkanime.dtos.SimulcastDto
import fr.shikkanime.entities.Simulcast
import fr.shikkanime.entities.enums.Season
import fr.shikkanime.factories.IGenericFactory

class SimulcastFactory : IGenericFactory<Simulcast, SimulcastDto> {
    override fun toDto(entity: Simulcast) = SimulcastDto(
        uuid = entity.uuid,
        season = entity.season!!,
        year = entity.year!!,
        slug = "${entity.season.name.lowercase()}-${entity.year}",
        label = when (entity.season) {
            Season.WINTER -> "Hiver"
            Season.SPRING -> "Printemps"
            Season.SUMMER -> "Été"
            Season.AUTUMN -> "Automne"
        } + " ${entity.year}",
    )
}