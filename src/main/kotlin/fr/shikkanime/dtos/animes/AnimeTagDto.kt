package fr.shikkanime.dtos.animes

import fr.shikkanime.dtos.TagDto
import java.io.Serializable
import java.util.*

data class AnimeTagDto(
    val uuid: UUID?,
    val tag: TagDto,
    val isAdult: Boolean,
    val isSpoiler: Boolean,
) : Serializable
