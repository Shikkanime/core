package fr.shikkanime.dtos.jais

import java.io.Serializable
import java.util.*

data class EpisodeTypeDto(
    val uuid: UUID,
    val name: String,
) : Serializable