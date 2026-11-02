package fr.shikkanime.database.entities

import fr.shikkanime.models.Image
import fr.shikkanime.models.ImageType
import fr.shikkanime.models.ModelConvertible
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.dao.UuidEntityClass
import org.jetbrains.exposed.v1.datetime.datetime
import kotlin.uuid.Uuid

const val IMAGE_TABLE_NAME = "image"
const val ENTITY_ID = "entity$ID"

internal object ImageTable : ShikkTable(IMAGE_TABLE_NAME) {
    val entityId = uuid(ENTITY_ID)
    val type = enumerationByName<ImageType>("type", 20)
    val url = varchar("url", 1000)

    // Materialization state, filled the first time the artwork is served. `version` is the short
    // hash of the stored payload: it addresses the image, so a refresh publishes a new URL.
    val version = varchar("version", 16).nullable()
    val fetchedAt = datetime("fetched_at").nullable()
    val sourceEtag = varchar("source_etag", 255).nullable()
    val sourceLastModified = varchar("source_last_modified", 64).nullable()
    val width = integer("width").nullable()
    val height = integer("height").nullable()
    val byteSize = long("byte_size").nullable()
    val failureCount = integer("failure_count").default(0)

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
    var version by ImageTable.version
    var fetchedAt by ImageTable.fetchedAt
    var sourceEtag by ImageTable.sourceEtag
    var sourceLastModified by ImageTable.sourceLastModified
    var width by ImageTable.width
    var height by ImageTable.height
    var byteSize by ImageTable.byteSize
    var failureCount by ImageTable.failureCount

    override fun toModel(): Image =
        Image(
            id = id.value,
            createdAt = createdAt,
            updatedAt = updatedAt,
            entityId = entityId,
            type = type,
            url = url,
            version = version,
            fetchedAt = fetchedAt,
            sourceEtag = sourceEtag,
            sourceLastModified = sourceLastModified,
            width = width,
            height = height
        )
}
