package fr.shikkanime.api.controllers

import fr.shikkanime.api.usecases.FindImageUseCase
import fr.shikkanime.api.usecases.ImageResult
import fr.shikkanime.ktor.ApiResponse
import fr.shikkanime.ktor.ApiResponses
import fr.shikkanime.ktor.GetMapping
import fr.shikkanime.ktor.HttpHeader
import fr.shikkanime.ktor.IController
import fr.shikkanime.ktor.Operation
import fr.shikkanime.ktor.PathParam
import fr.shikkanime.ktor.ResponseEntity
import fr.shikkanime.ktor.RestController
import fr.shikkanime.ktor.dtos.ErrorMessageDto
import fr.shikkanime.ktor.dtos.MessageDto
import fr.shikkanime.models.ImageType
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import org.koin.core.annotation.Single
import kotlin.uuid.Uuid

private const val EXTENSION = ".webp"
private const val WILDCARD_ETAG = "*"
private const val WEAK_PREFIX = "W/"
private const val IMMUTABLE_CACHE_CONTROL = "public, max-age=31536000, immutable"
private val WEBP_CONTENT_TYPE: ContentType = ContentType.parse("image/webp")

/**
 * Serves the artwork of every entity, by version.
 *
 * The address carries the version of the payload, so the bytes behind it never change: the response
 * is immutable for a year, and a refreshed artwork publishes a new address instead of invalidating
 * the previous one. What the request means — materialize, redirect, refuse — belongs to
 * [FindImageUseCase]; this controller only turns it into HTTP.
 *
 * @param findImageUseCase use case resolving the artwork of a request.
 * @see [FindImageUseCase]
 */
@Suppress("unused") // Bound by Koin and routed from its annotations
@Single(binds = [IController::class])
@RestController("/v1/images")
class ImageController(
    private val findImageUseCase: FindImageUseCase
) : IController {

    /**
     * Serves one artwork of an entity, materializing it when it was never asked for.
     *
     * @param entityId identifier of the owning entity.
     * @param type type of the artwork, converted by the binder.
     * @param file file name of the artwork, `<version>.webp`.
     * @param ifNoneMatch validator the client already holds, when it revalidates.
     * @return the WebP payload, a redirect to the current version, or an error.
     * @see [FindImageUseCase]
     */
    @GetMapping("/{entityId}/{type}/{file}")
    @Operation(
        summary = "Serve an artwork",
        description = "Serves the WebP artwork of an entity at its published version, " +
            "materializing it on first request",
        tags = ["Image"]
    )
    @ApiResponses([
        ApiResponse(200, "The artwork of the requested version", ByteArray::class),
        ApiResponse(302, "The requested version is superseded", String::class),
        ApiResponse(304, "The client already holds the current version", String::class),
        ApiResponse(400, "Invalid image type or version", ErrorMessageDto::class),
        ApiResponse(404, "The entity has no such artwork", ErrorMessageDto::class),
        ApiResponse(503, "The platform could not be read", ErrorMessageDto::class),
    ])
    suspend fun findImage(
        @PathParam("entityId") entityId: Uuid,
        @PathParam("type") type: ImageType,
        @PathParam("file") file: String,
        @HttpHeader(HttpHeaders.IfNoneMatch, required = false) ifNoneMatch: String?
    ): ResponseEntity<*> {
        val version = file.removeSuffix(EXTENSION)
        if (version == file || version.isBlank())
            return ResponseEntity.badRequest(MessageDto.error("An artwork address ends with $EXTENSION"))

        val clientHolds = matchesIfNoneMatch(ifNoneMatch, version)

        return when (val result = findImageUseCase.find(entityId, type, version, clientHolds)) {
            is ImageResult.Found -> ResponseEntity.binary(
                result.payload,
                WEBP_CONTENT_TYPE,
                etag = result.version,
                cacheControl = IMMUTABLE_CACHE_CONTROL
            )

            is ImageResult.NotModified -> ResponseEntity.notModified(result.version)

            is ImageResult.Moved -> ResponseEntity.redirect(addressOf(entityId, type, result.version))

            ImageResult.NotFound -> ResponseEntity.notFound(MessageDto.error("Image not found"))

            ImageResult.Unavailable ->
                ResponseEntity(MessageDto.error("Image unavailable"), HttpStatusCode.ServiceUnavailable)
        }
    }

    /**
     * Builds the address of a published artwork version.
     *
     * @param entityId identifier of the owning entity.
     * @param type type of the artwork.
     * @param version published version of the payload.
     * @return the address serving that version.
     * @see [FindImageUseCase]
     */
    private fun addressOf(entityId: Uuid, type: ImageType, version: String): String =
        "/v1/images/$entityId/${type.name}/$version$EXTENSION"

    /**
     * Tells whether a client validator covers the published version.
     *
     * `If-None-Match` carries a list, may be the wildcard, and may be a weak tag: an exact string
     * comparison would answer `200` to a client that already holds the bytes.
     *
     * @param ifNoneMatch raw header sent by the client, when it revalidates.
     * @param version published version of the payload.
     * @return `true` when the client already holds this version.
     * @see [FindImageUseCase]
     */
    private fun matchesIfNoneMatch(ifNoneMatch: String?, version: String): Boolean {
        if (ifNoneMatch == null)
            return false

        if (ifNoneMatch.trim() == WILDCARD_ETAG)
            return true

        return ifNoneMatch.split(",").any { it.trim().removePrefix(WEAK_PREFIX) == etagOf(version) }
    }

    /**
     * Formats a version as the entity tag published with it.
     *
     * @param version published version of the payload.
     * @return the entity tag a client echoes back when it revalidates.
     * @see [FindImageUseCase]
     */
    private fun etagOf(version: String): String = "\"$version\""
}
