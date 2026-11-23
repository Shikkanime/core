package fr.shikkanime.models

import kotlinx.datetime.LocalDateTime
import kotlin.uuid.Uuid

/**
 * Image asset attached to any entity, stored once per owner and [ImageType].
 *
 * A [manual] image was set by an administrator: automatic writes, such as a platform harvest,
 * never replace its URL. Only another manual write or an explicit release changes it back.
 */
data class Image(
    override val id: Uuid?,
    override val createdAt: LocalDateTime?,
    override val updatedAt: LocalDateTime?,
    val entityId: Uuid?,
    val type: ImageType,
    val url: String,
    val manual: Boolean = false
) : ShikkModel(id, createdAt, updatedAt)
