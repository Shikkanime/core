package fr.shikkanime.models

import kotlinx.datetime.LocalDateTime
import kotlin.uuid.Uuid

data class AnimeLocalization(
    override val id: Uuid,
    override val createdAt: LocalDateTime,
    override val updatedAt: LocalDateTime,
    val locale: String,
    val name: String,
    val slug: String,
    val description: String?
) : ShikkModel(id, createdAt, updatedAt)
