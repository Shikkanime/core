package fr.shikkanime.dtos.jais

import java.io.Serializable
import java.util.*

data class SimulcastDto(
    val uuid: UUID,
    val season: String,
    val year: Int,
) : Serializable