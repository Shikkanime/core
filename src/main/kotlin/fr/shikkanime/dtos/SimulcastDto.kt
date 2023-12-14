package fr.shikkanime.dtos

import java.io.Serializable
import java.util.*

data class SimulcastDto(
    val uuid: UUID?,
    val season: String,
    val year: Int,
) : Serializable
