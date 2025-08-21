package fr.shikkanime.dtos

import fr.shikkanime.entities.enums.Season
import java.io.Serializable
import java.util.*

data class SimulcastDto(
    val uuid: UUID?,
    val season: Season,
    val year: Int,
    val slug: String,
    val label: String,
    var lastReleaseDateTime: String? = null,
    var animesCount: Long? = null
) : Serializable