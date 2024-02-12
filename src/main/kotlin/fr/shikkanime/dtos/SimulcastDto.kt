package fr.shikkanime.dtos

import java.io.Serializable
import java.util.*

data class SimulcastDto(
    val uuid: UUID?,
    val season: String,
    val year: Int,
    val slug: String,
    val label: String,
    val lastReleaseDateTime: String? = null
) : Serializable
