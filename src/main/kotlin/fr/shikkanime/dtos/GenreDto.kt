package fr.shikkanime.dtos

import java.io.Serializable
import java.util.*

data class GenreDto(
    val uuid: UUID?,
    val name: String,
) : Serializable
