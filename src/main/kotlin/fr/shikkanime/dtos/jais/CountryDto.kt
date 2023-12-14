package fr.shikkanime.dtos.jais

import java.io.Serializable
import java.util.*

data class CountryDto(
    val uuid: UUID,
    val tag: String,
    val name: String,
) : Serializable