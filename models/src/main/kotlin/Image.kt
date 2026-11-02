package fr.shikkanime.models

import kotlinx.datetime.LocalDateTime
import kotlin.uuid.Uuid

/**
 * Image asset attached to any entity, stored once per owner and [ImageType].
 *
 * The row carries the source [url] of the artwork and, once the artwork has been materialized, the
 * [version] of the stored WebP payload: the served URL embeds that version, so a refreshed image
 * gets a new address and every cache in front of it becomes stale for free.
 */
data class Image(
    override val id: Uuid?,
    override val createdAt: LocalDateTime?,
    override val updatedAt: LocalDateTime?,
    val entityId: Uuid?,
    val type: ImageType,
    val url: String,
    val version: String? = null,
    val fetchedAt: LocalDateTime? = null,
    val sourceEtag: String? = null,
    val sourceLastModified: String? = null,
    val width: Int? = null,
    val height: Int? = null
) : ShikkModel(id, createdAt, updatedAt)
