package fr.shikkanime.api.controllers

import fr.shikkanime.api.images.WebpConversionException
import fr.shikkanime.api.usecases.ConvertImageToWebpUseCase
import fr.shikkanime.ktor.ApiResponse
import fr.shikkanime.ktor.ApiResponses
import fr.shikkanime.ktor.GetMapping
import fr.shikkanime.ktor.IController
import fr.shikkanime.ktor.Operation
import fr.shikkanime.ktor.QueryParam
import fr.shikkanime.ktor.ResponseEntity
import fr.shikkanime.ktor.RestController
import fr.shikkanime.ktor.dtos.ErrorMessageDto
import fr.shikkanime.ktor.dtos.MessageDto
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import org.koin.core.annotation.Single

private const val DEFAULT_QUALITY = 80
private const val NO_STORE_CACHE_CONTROL = "no-store"
private val WEBP_CONTENT_TYPE: ContentType = ContentType.parse("image/webp")

/**
 * Converts a picture published on the web to WebP.
 *
 * The endpoint exists to exercise the WebP plugin on real pictures: nothing is stored, nothing is
 * cached, and the answer is the encoded payload itself.
 *
 * @param convertImageToWebpUseCase use case reading and converting the picture.
 * @see [ConvertImageToWebpUseCase]
 */
@Suppress("unused")
@RestController("/v1/webp")
@Single(binds = [IController::class])
class WebpController(
    private val convertImageToWebpUseCase: ConvertImageToWebpUseCase
) : IController {
    /**
     * Converts the picture published at [url].
     *
     * @param url absolute http or https address of the picture.
     * @param quality quality factor between 0 and 100, 80 when absent.
     * @return the encoded WebP payload, or an error.
     * @see [ConvertImageToWebpUseCase.convert]
     */
    @GetMapping("/convert")
    @Operation(
        summary = "Convert a picture to WebP",
        description = "Reads the picture published at the given address and answers its WebP encoding",
        tags = ["WebP"]
    )
    @ApiResponses([
        ApiResponse(200, "The encoded picture", ByteArray::class),
        ApiResponse(400, "Invalid address or quality", ErrorMessageDto::class),
        ApiResponse(502, "The picture could not be read or encoded", ErrorMessageDto::class),
    ])
    suspend fun convert(
        @QueryParam("url") url: String,
        @QueryParam("quality", required = false) quality: Int? = null
    ): ResponseEntity<*> =
        try {
            val payload = convertImageToWebpUseCase.convert(url, quality ?: DEFAULT_QUALITY)

            ResponseEntity.binary(
                payload,
                WEBP_CONTENT_TYPE,
                cacheControl = NO_STORE_CACHE_CONTROL
            )
        } catch (exception: WebpConversionException) {
            ResponseEntity(MessageDto.error(exception.message ?: "Conversion failed"), HttpStatusCode.BadGateway)
        }
}
