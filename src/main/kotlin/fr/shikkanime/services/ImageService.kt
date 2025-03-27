package fr.shikkanime.services

import fr.shikkanime.entities.Attachment
import fr.shikkanime.entities.enums.ImageType
import fr.shikkanime.utils.Constant
import fr.shikkanime.utils.FileManager
import java.io.Serializable
import java.time.Instant
import java.time.ZonedDateTime
import java.util.*

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

        Constant.imagesFolder.walk()
            .filter { it.isFile && it.extension == "shikk" && it.nameWithoutExtension.contains("_") }
            .forEach { file ->
                val media = FileManager.readFile<Media>(file)

                val attachment = attachmentService.save(
                    Attachment(
                        lastUpdateDateTime = ZonedDateTime.ofInstant(Instant.ofEpochMilli(media.lastUpdateDateTime ?: System.currentTimeMillis()), Constant.utcZoneId),
                        entityUuid = media.uuid,
                        type = media.type,
                        url = media.url,
                    )
                )

                val attachmentFile = attachmentService.getFile(attachment)
                attachmentFile.writeBytes(media.bytes)

                file.delete()
            }
    }
}