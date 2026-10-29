package fr.shikkanime.api.controllers

import fr.shikkanime.api.usecases.GetAttachmentResult
import fr.shikkanime.api.usecases.GetAttachmentUseCase
import fr.shikkanime.ktor.ApiResponse
import fr.shikkanime.ktor.ApiResponses
import fr.shikkanime.ktor.GetMapping
import fr.shikkanime.ktor.HttpHeader
import fr.shikkanime.ktor.IController
import fr.shikkanime.ktor.Operation
import fr.shikkanime.ktor.QueryParam
import fr.shikkanime.ktor.ResponseEntity
import fr.shikkanime.ktor.RestController
import fr.shikkanime.ktor.dtos.ErrorMessageDto
import fr.shikkanime.ktor.dtos.MessageDto
import fr.shikkanime.models.ImageType
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import org.koin.core.annotation.Single
import kotlin.uuid.Uuid

private const val CACHE_CONTROL = "public, max-age=86400"
private val WEBP_CONTENT_TYPE: ContentType = ContentType.parse("image/webp")

/**
 * Serves the artwork stored for an entity, encoded as WebP.
 *
 * The route follows the historical attachment API: a single `GET` carrying the entity identifier
 * and the image type, answering the bytes with an `ETag` the client can revalidate.
 *
 * @param getAttachmentUseCase workflow resolving and encoding the stored artwork.
 * @see [GetAttachmentUseCase]
 */
@Suppress("unused")
@RestController("/v1/attachments")
@Single(binds = [IController::class])
class AttachmentController(
    private val getAttachmentUseCase: GetAttachmentUseCase
) : IController {
    /**
     * Reads the WebP rendition of an attachment.
     *
     * @param uuid identifier of the entity owning the artwork.
     * @param type type of the artwork to read.
     * @param ifNoneMatch validator sent by the client, when present.
     * @return the WebP payload, a 304, or an error.
     * @see [GetAttachmentUseCase.execute]
     */
    @GetMapping
    @Operation(
        summary = "Read an attachment",
        description = "Returns the stored artwork of an entity and type, encoded as WebP",
        tags = ["Attachments"]
    )
    @ApiResponses([
        ApiResponse(200, "The encoded artwork", ByteArray::class),
        ApiResponse(304, "The artwork has not changed", ByteArray::class),
        ApiResponse(400, "Invalid uuid or type", ErrorMessageDto::class),
        ApiResponse(404, "No artwork for this entity and type", ErrorMessageDto::class),
        ApiResponse(502, "The source artwork could not be read or encoded", ErrorMessageDto::class)
    ])
    suspend fun get(
        @QueryParam("uuid", required = false) uuid: Uuid?,
        @QueryParam("type", required = false) type: ImageType?,
        @HttpHeader("If-None-Match", required = false) ifNoneMatch: String?
    ): ResponseEntity<*> {
        if (uuid == null)
            return ResponseEntity.badRequest(MessageDto.error("UUID is required"))

        if (type == null)
            return ResponseEntity.badRequest(MessageDto.error("Invalid type"))

        return when (val result = getAttachmentUseCase.execute(uuid, type, ifNoneMatch)) {
            is GetAttachmentResult.Found ->
                ResponseEntity.binary(result.payload, WEBP_CONTENT_TYPE, result.etag, CACHE_CONTROL)

            is GetAttachmentResult.NotModified ->
                ResponseEntity.notModified(result.etag)

            GetAttachmentResult.NotFound ->
                ResponseEntity.notFound(MessageDto.error("Attachment not found"))

            is GetAttachmentResult.Unavailable ->
                ResponseEntity(MessageDto.error(result.message), HttpStatusCode.BadGateway)
        }
    }
}
