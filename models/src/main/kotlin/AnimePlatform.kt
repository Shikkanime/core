package fr.shikkanime.models

import kotlinx.datetime.LocalDateTime
import kotlin.uuid.Uuid

data class AnimePlatform(
    override val id: Uuid,
    override val createdAt: LocalDateTime,
    override val updatedAt: LocalDateTime,
    val platform: Platform,
    val platformId: String
) : ShikkModel(id, createdAt, updatedAt)
