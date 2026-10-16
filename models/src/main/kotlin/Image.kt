package fr.shikkanime.models

import kotlinx.datetime.LocalDateTime
import kotlin.uuid.Uuid

/**
 * Image asset attached to any entity, stored once per owner and [ImageType].
 *
 * @property entityId Identifier of the owning entity (anime, member, ...).
 * @property type Role of the image for its owner.
 * @property url Direct platform or CDN URL of the image.
 */
data class Image(
    override val id: Uuid?,
    override val createdAt: LocalDateTime?,
    override val updatedAt: LocalDateTime?,
    val entityId: Uuid?,
    val type: ImageType,
    val url: String
) : ShikkModel(id, createdAt, updatedAt)
