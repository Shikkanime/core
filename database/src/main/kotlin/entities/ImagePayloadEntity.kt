package fr.shikkanime.database.entities

import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.UuidTable
import org.jetbrains.exposed.v1.dao.UuidEntity
import org.jetbrains.exposed.v1.dao.UuidEntityClass
import kotlin.uuid.Uuid

const val IMAGE_PAYLOAD_TABLE_NAME = "image_payload"

/**
 * Encoded payload of an image, in its own table so a catalogue read never loads the bytes.
 *
 * The row identifier is the identifier of the image it belongs to: one payload per image, replaced
 * in place when the artwork changes.
 */
internal object ImagePayloadTable : UuidTable(IMAGE_PAYLOAD_TABLE_NAME) {
    val bytes = binary("bytes")
}

internal class ImagePayloadEntity(id: EntityID<Uuid>) : UuidEntity(id) {
    companion object : UuidEntityClass<ImagePayloadEntity>(ImagePayloadTable)

    var bytes by ImagePayloadTable.bytes
}
