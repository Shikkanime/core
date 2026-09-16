package fr.shikkanime.models

import kotlinx.datetime.LocalDateTime
import kotlin.uuid.Uuid

data class Episode(
    override val id: Uuid,
    override val createdAt: LocalDateTime,
    override val updatedAt: LocalDateTime,
    val platform: Platform,
    val platformEpisodeId: String,
    val title: String,
    val releaseDateTime: LocalDateTime
) : ShikkModel(id, createdAt, updatedAt)
