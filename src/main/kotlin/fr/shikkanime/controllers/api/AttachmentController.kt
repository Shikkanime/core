package fr.shikkanime.controllers.api

import fr.shikkanime.dtos.MessageDto
import fr.shikkanime.entities.enums.ImageType
import fr.shikkanime.services.ImageService
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
    @Path
    @Get
    @Cached(maxAgeSeconds = Constant.DEFAULT_CACHE_DURATION)
    private fun getAttachment(
        @QueryParam("uuid")
        uuid: UUID?,
        @QueryParam("type")
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

        val tmpTypes = buildSet {
            ImageType.entries.find { it.name.equals(typeString, true) }?.let { add(it) }
            add(ImageType.THUMBNAIL)
            add(ImageType.BANNER)
            add(ImageType.MEMBER_PROFILE)
        }

        val image = tmpTypes.asSequence()
            .mapNotNull { ImageService[uuid, it] }
            .firstOrNull() ?: return Response.notFound(
            MessageDto(
                MessageDto.Type.ERROR,
                "Attachment not found"
            )
        )

        return Response.multipart(image.bytes, ContentType.parse("image/webp"))
    }
}