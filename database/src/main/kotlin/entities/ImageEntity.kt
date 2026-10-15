package fr.shikkanime.database.entities

import fr.shikkanime.models.Image
import fr.shikkanime.models.ImageType
import fr.shikkanime.models.ModelConvertible
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.dao.UuidEntityClass
import kotlin.uuid.Uuid

const val IMAGE_TABLE_NAME = "image"
const val ENTITY_ID = "entity$ID"

internal object ImageTable : ShikkTable(IMAGE_TABLE_NAME) {
    val entityId = uuid(ENTITY_ID)
    val type = enumerationByName<ImageType>("type", 20)
    val url = varchar("url", 1000)

    init {
        uniqueIndex("${ENTITY_ID}_type$UQ", entityId, type)
    }
}

internal class ImageEntity(id: EntityID<Uuid>) :
    ShikkEntity(id, ImageTable),
    ModelConvertible<Image> {
    companion object : UuidEntityClass<ImageEntity>(ImageTable)

    var entityId by ImageTable.entityId
    var type by ImageTable.type
    var url by ImageTable.url

    override fun toModel(): Image =
        Image(
            id = id.value,
            createdAt = createdAt,
            updatedAt = updatedAt,
            entityId = entityId,
            type = type,
            url = url
        )
}
