package fr.shikkanime.dtos.simulcasts

import java.util.*

data class SimulcastDto(
    val uuid: UUID?,
    val season: String,
    val year: Int,
    val slug: String,
    val label: String,
    var lastReleaseDateTime: String? = null,
    var animesCount: Long? = null
)