package fr.shikkanime.services

import fr.shikkanime.entities.Attachment
import fr.shikkanime.entities.enums.ImageType
import fr.shikkanime.utils.Constant
import fr.shikkanime.utils.FileManager
import java.io.Serializable
import java.nio.file.Files
import java.nio.file.attribute.BasicFileAttributes
import java.time.Instant
import java.time.ZonedDateTime
import java.util.*

@Deprecated("Use AttachmentService instead")
object ImageService {
    data class Media(
        val uuid: UUID,
        val type: ImageType,
        var url: String? = null,
        var bytes: ByteArray = byteArrayOf(),
        var originalSize: Int = 0,
        var lastUpdateDateTime: Long? = null
    ) : Serializable {
        companion object {
            const val serialVersionUID: Long = 0
        }
    }

    fun convertFilesToAttachment() {
        val attachmentService = Constant.injector.getInstance(AttachmentService::class.java)
        val attachments = attachmentService.findAll()
            .groupBy { it.entityUuid!! to it.type!! }

        Constant.imagesFolder.walk()
            .filter { it.isFile && it.extension == "shikk" && "_" in it.nameWithoutExtension }
            .forEach { file ->
                val media = FileManager.readFile<Media>(file)
                val attributes = Files.readAttributes(file.toPath(), BasicFileAttributes::class.java)
                val mediaAttachments = attachments[media.uuid to media.type] ?: emptyList()

                val attachment = when (mediaAttachments.size) {
                    1 -> mediaAttachments.first()
                    0 -> Attachment(entityUuid = media.uuid, type = media.type)
                    else -> error("Multiple attachments found for ${media.uuid} and type ${media.type}")
                }

                attachment.apply {
                    creationDateTime = ZonedDateTime.ofInstant(Instant.ofEpochMilli(attributes.creationTime().toMillis()), Constant.utcZoneId)
                    lastUpdateDateTime = ZonedDateTime.ofInstant(Instant.ofEpochMilli(media.lastUpdateDateTime ?: System.currentTimeMillis()), Constant.utcZoneId)
                    url = media.url
                }

                val savedOrUpdated = if (attachment.uuid == null) {
                    attachmentService.save(attachment)
                } else {
                    attachmentService.update(attachment)
                }

                attachmentService.getFile(savedOrUpdated).writeBytes(media.bytes)
                file.delete()
            }
    }
}