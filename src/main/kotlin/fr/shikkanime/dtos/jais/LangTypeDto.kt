package fr.shikkanime.dtos.jais

import java.io.Serializable
import java.util.*

data class LangTypeDto(
    val uuid: UUID,
    val name: String,
) : Serializable