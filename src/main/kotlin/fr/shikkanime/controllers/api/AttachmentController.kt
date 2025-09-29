package fr.shikkanime.controllers.api

import com.google.inject.Inject
import fr.shikkanime.dtos.MessageDto
import fr.shikkanime.entities.enums.ImageType
import fr.shikkanime.services.AttachmentService
import fr.shikkanime.services.caches.AttachmentCacheService
import fr.shikkanime.utils.Constant
import fr.shikkanime.utils.routes.Cached
import fr.shikkanime.utils.routes.Controller
import fr.shikkanime.utils.routes.Path
import fr.shikkanime.utils.routes.Response
import fr.shikkanime.utils.routes.method.Get
import fr.shikkanime.utils.routes.param.QueryParam
import io.ktor.http.*
import java.util.*

@Controller("/api/v1/attachments")
class AttachmentController {
    @Inject private lateinit var attachmentService: AttachmentService
    @Inject private lateinit var attachmentCacheService: AttachmentCacheService

    @Path
    @Get
    @Cached(maxAgeSeconds = Constant.DEFAULT_CACHE_DURATION)
    private fun getAttachment(
        @QueryParam uuid: UUID?,
        @QueryParam("type") typeString: String?
    ): Response {
        if (uuid == null || runCatching { UUID.fromString(uuid.toString()) }.isFailure)
            Response.badRequest(MessageDto.error("UUID is required"))

        val type = ImageType.entries.find { it.name.equals(typeString, true) }
            ?: return Response.badRequest(MessageDto.error("Invalid type"))

        // Get attachment from cache or database
        val attachment = attachmentCacheService.findByEntityUuidTypeAndActive(uuid!!, type)
            ?: return Response.notFound(MessageDto.error("Attachment not found"))

        // Check if image is in the memory cache
        val imageBytes = attachmentService.getContentFromCache(attachment) ?: run {
            val file = attachmentService.getFile(attachment)

            if (!file.exists() || file.length() <= 0 || attachment.uuid in attachmentService.inProgressAttachments)
                return Response.notFound(MessageDto.error("Attachment not found"))

            // Read the file and store it in the cache
            val bytes = file.readBytes()
            attachmentService.setContentInCache(attachment, bytes)
            bytes
        }

        return Response.multipart(imageBytes, ContentType.Image.WEBP)
    }
}