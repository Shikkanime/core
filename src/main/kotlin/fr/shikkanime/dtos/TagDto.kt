package fr.shikkanime.dtos

import java.io.Serializable
import java.util.*

data class TagDto(
    val uuid: UUID?,
    val name: String,
) : Serializable
