package fr.shikkanime.dtos.simulcasts

import java.util.UUID

data class SimulcastDto(
    val uuid: UUID?,
    val season: String,
    val year: Int,
    val slug: String,
    val label: String,
    var lastReleaseDateTime: String? = null
)