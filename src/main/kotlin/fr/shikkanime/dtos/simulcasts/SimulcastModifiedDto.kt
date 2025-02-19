package fr.shikkanime.dtos.simulcasts

import fr.shikkanime.entities.Simulcast
import java.time.ZonedDateTime

data class SimulcastModifiedDto(
    val simulcast: Simulcast,
    val lastReleaseDateTime: ZonedDateTime,
    val animesCount: Long
)
