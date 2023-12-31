package fr.shikkanime.dtos.jais

import java.io.Serializable
import java.util.*

data class PlatformDto(
    val uuid: UUID,
    val name: String,
    val url: String,
    val image: String,
) : Serializable