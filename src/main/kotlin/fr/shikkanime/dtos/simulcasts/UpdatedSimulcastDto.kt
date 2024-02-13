package fr.shikkanime.dtos.simulcasts

import java.io.Serializable
import java.util.*

data class UpdatedSimulcastDto(
    val uuid: UUID?,
    val season: String,
    val year: Int,
    val slug: String,
    val label: String,
    val lastReleaseDateTime: String? = null
) : Serializable
