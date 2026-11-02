package fr.shikkanime.api.images

import fr.shikkanime.core.LoggerFactory
import fr.shikkanime.ktor.createHttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsChannel
import io.ktor.utils.io.readBuffer
import kotlinx.coroutines.CancellationException
import kotlinx.io.readByteArray
import io.ktor.http.HttpHeaders
import io.ktor.http.contentLength
import io.ktor.http.HttpStatusCode
import org.koin.core.annotation.Single

private const val MAX_PAYLOAD_BYTES = 10 * 1024 * 1024
private const val HTTPS_PREFIX = "https://"
private const val USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
    "(KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36"

/**
 * Reads artwork from the platforms, through the Ktor client of the framework.
 *
 * The client is created once and shared: it carries the request, connection and socket timeouts,
 * the redirect handling and the content negotiation of the framework. A read is conditional when
 * the stored artwork has validators, so an untouched platform answers `304` and transfers no bytes.
 *
 * @see [ImageFetch]
 */
@Single
internal class ImageFetcher {
    private val logger = LoggerFactory.getLogger()
    private val client = createHttpClient()

    /**
     * Reads an artwork from its platform.
     *
     * @param url source URL of the artwork.
     * @param etag validator of the stored payload, when the platform provided one.
     * @param lastModified modification date of the stored payload, when provided.
     * @return the fetched bytes, the confirmation that nothing changed, or a failure.
     * @see [ImageFetch]
     */
    suspend fun fetch(url: String, etag: String?, lastModified: String?): ImageFetch =
        if (!url.startsWith(HTTPS_PREFIX)) {
            logger.warning("Refusing a non-https artwork URL '$url'")
            ImageFetch.Failed
        } else try {
            val response = client.get(url) {
                header(HttpHeaders.UserAgent, USER_AGENT)
                etag?.let { header(HttpHeaders.IfNoneMatch, it) }
                lastModified?.let { header(HttpHeaders.IfModifiedSince, it) }
            }

            when (response.status) {
                HttpStatusCode.NotModified -> ImageFetch.NotModified(
                    response.headers[HttpHeaders.ETag],
                    response.headers[HttpHeaders.LastModified]
                )

                HttpStatusCode.OK -> {
                    // Refuse a declared oversize before pulling the body into the heap
                    val declared = response.contentLength() ?: 0L
                    // Bound the read itself: a chunked response declares no length
                    val bytes = if (declared > MAX_PAYLOAD_BYTES)
                        ByteArray(0)
                    else
                        response.bodyAsChannel()
                            .readBuffer(MAX_PAYLOAD_BYTES + 1L)
                            .readByteArray()

                    if (bytes.isEmpty() || bytes.size > MAX_PAYLOAD_BYTES) {
                        logger.warning("Artwork '$url' exceeds $MAX_PAYLOAD_BYTES bytes")
                        ImageFetch.Failed
                    } else {
                        ImageFetch.Fetched(
                            bytes,
                            response.headers[HttpHeaders.ETag],
                            response.headers[HttpHeaders.LastModified]
                        )
                    }
                }

                else -> {
                    logger.warning("Artwork '$url' answered ${response.status}")
                    ImageFetch.Failed
                }
            }
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            // Any transport failure — refused, timed out, unreachable — is an unavailable artwork
            logger.warning("Could not read the artwork '$url': ${exception.message}")
            ImageFetch.Failed
        }
}
