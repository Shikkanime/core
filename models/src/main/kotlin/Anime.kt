package fr.shikkanime.models

import kotlinx.datetime.LocalDateTime
import kotlin.uuid.Uuid

data class Anime(
    override val id: Uuid,
    override val createdAt: LocalDateTime,
    override val updatedAt: LocalDateTime,
    val localizations: List<AnimeLocalization>,
    val platforms: List<AnimePlatform>
) : ShikkModel(id, createdAt, updatedAt)
