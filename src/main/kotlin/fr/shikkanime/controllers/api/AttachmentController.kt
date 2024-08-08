package fr.shikkanime.controllers.api

import fr.shikkanime.dtos.MessageDto
import fr.shikkanime.services.ImageService
import fr.shikkanime.utils.Constant
import fr.shikkanime.utils.routes.Cached
import fr.shikkanime.utils.routes.Controller
import fr.shikkanime.utils.routes.Path
import fr.shikkanime.utils.routes.Response
import fr.shikkanime.utils.routes.method.Get
import fr.shikkanime.utils.routes.openapi.OpenAPI
import fr.shikkanime.utils.routes.openapi.OpenAPIResponse
import fr.shikkanime.utils.routes.param.QueryParam
import io.ktor.http.*
import java.util.*

@Controller("/api/v1/attachments")
class AttachmentController {
    @Path
    @Get
    @Cached(maxAgeSeconds = Constant.DEFAULT_CACHE_DURATION)
    @OpenAPI(
        "Get attachment",
        [
            OpenAPIResponse(
                200,
                "Attachment found",
                ByteArray::class,
                "image/webp"
            ),
            OpenAPIResponse(
                404,
                "Attachment not found",
                MessageDto::class,
            ),
        ]
    )
    private fun getAttachment(
        @QueryParam(
            "uuid",
            description = "UUID of the attachment",
            required = true,
            type = UUID::class
        )
        uuid: UUID?,
        @QueryParam(
            "type",
            description = "Type of the attachment",
            example = "IMAGE",
            type = ImageService.Type::class
        )
        typeString: String?
    ): Response {
        if (uuid == null) {
            return Response.badRequest(
                MessageDto(
                    MessageDto.Type.ERROR,
                    "UUID is required"
                )
            )
        }

        val type = ImageService.Type.entries.find { it.name.equals(typeString, true) } ?: ImageService.Type.IMAGE

        val image = ImageService[uuid, type] ?: return Response.notFound(
            MessageDto(
                MessageDto.Type.ERROR,
                "Attachment not found"
            )
        )

        return Response.multipart(image.bytes, ContentType.parse("image/webp"))
    }
}