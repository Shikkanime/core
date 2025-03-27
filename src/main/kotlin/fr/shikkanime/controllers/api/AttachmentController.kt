package fr.shikkanime.controllers.api

import com.google.inject.Inject
import fr.shikkanime.dtos.MessageDto
import fr.shikkanime.entities.enums.ImageType
import fr.shikkanime.services.AttachmentService
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
    @Inject
    private lateinit var attachmentService: AttachmentService

    @Path
    @Get
    @Cached(maxAgeSeconds = Constant.DEFAULT_CACHE_DURATION)
    private fun getAttachment(
        @QueryParam("uuid")
        uuid: UUID?,
        @QueryParam("type")
        typeString: String?
    ): Response {
        if (uuid == null || runCatching { UUID.fromString(uuid.toString()) }.isFailure) {
            return Response.badRequest(
                MessageDto(
                    MessageDto.Type.ERROR,
                    "UUID is required"
                )
            )
        }

        val type = ImageType.entries.find { it.name.equals(typeString, true) } ?: return Response.badRequest(
            MessageDto(
                MessageDto.Type.ERROR,
                "Invalid type"
            )
        )

        val attachment = attachmentService.findByEntityUuidTypeAndActive(uuid, type) ?: return Response.notFound(
            MessageDto(
                MessageDto.Type.ERROR,
                "Attachment not found"
            )
        )

        return Response.multipart(attachmentService.getFile(attachment).readBytes(), ContentType.parse("image/webp"))
    }
}