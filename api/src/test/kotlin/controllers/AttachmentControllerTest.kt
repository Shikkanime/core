package fr.shikkanime.api.controllers

import fr.shikkanime.api.usecases.GetAttachmentResult
import fr.shikkanime.api.usecases.GetAttachmentUseCase
import fr.shikkanime.ktor.ControllerBinder
import fr.shikkanime.ktor.configureDefaultModules
import fr.shikkanime.models.ImageType
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsBytes
import io.ktor.http.*
import io.ktor.server.application.Application
import io.ktor.server.routing.routing
import io.ktor.server.testing.testApplication
import io.mockk.coEvery
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.extension.ExtendWith
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.uuid.Uuid

@ExtendWith(MockKExtension::class)
@DisplayName("tests for the attachment controller")
class AttachmentControllerTest {
    @MockK private lateinit var getAttachmentUseCase: GetAttachmentUseCase
    @InjectMockKs private lateinit var attachmentController: AttachmentController

    private val entityId = Uuid.random()
    private val payload = byteArrayOf(0x52, 0x49, 0x46, 0x46)
    private val etag = "validator-1"

    /**
     * Registers the controller under test on the test application.
     *
     * @receiver the Ktor test application.
     * @see [ControllerBinder]
     */
    private fun Application.registerControllers() {
        routing {
            ControllerBinder.register(this, listOf(attachmentController))
        }
    }

    @Nested
    @DisplayName("tests for reading an attachment")
    inner class GetTests {
        @Test
        @DisplayName("should return the WebP payload with its cache headers")
        fun `should return the WebP payload with its cache headers`() =
            testApplication {
                // Given
                coEvery {
                    getAttachmentUseCase.execute(entityId, ImageType.THUMBNAIL, null)
                } returns GetAttachmentResult.Found(payload, etag)
                application {
                    configureDefaultModules()
                    registerControllers()
                }

                // When
                val response = client.get("/v1/attachments?uuid=$entityId&type=THUMBNAIL")

                // Then
                assertEquals(HttpStatusCode.OK, response.status)
                assertContentEquals(payload, response.bodyAsBytes())
                assertEquals(
                    ContentType.parse("image/webp"),
                    response.contentType()?.withoutParameters()
                )
                assertEquals("\"$etag\"", response.headers[HttpHeaders.ETag])
                assertEquals("public, max-age=86400", response.headers[HttpHeaders.CacheControl])
            }

        @Test
        @DisplayName("should return 304 when the validator matches")
        fun `should return 304 when the validator matches`() =
            testApplication {
                // Given
                coEvery {
                    getAttachmentUseCase.execute(entityId, ImageType.THUMBNAIL, "\"$etag\"")
                } returns GetAttachmentResult.NotModified(etag)
                application {
                    configureDefaultModules()
                    registerControllers()
                }

                // When
                val response = client.get("/v1/attachments?uuid=$entityId&type=THUMBNAIL") {
                    header(HttpHeaders.IfNoneMatch, "\"$etag\"")
                }

                // Then
                assertEquals(HttpStatusCode.NotModified, response.status)
                assertEquals("\"$etag\"", response.headers[HttpHeaders.ETag])
            }

        @Test
        @DisplayName("should return 404 when no image is stored")
        fun `should return 404 when no image is stored`() =
            testApplication {
                // Given
                coEvery {
                    getAttachmentUseCase.execute(entityId, ImageType.BANNER, null)
                } returns GetAttachmentResult.NotFound
                application {
                    configureDefaultModules()
                    registerControllers()
                }

                // When
                val response = client.get("/v1/attachments?uuid=$entityId&type=BANNER")

                // Then
                assertEquals(HttpStatusCode.NotFound, response.status)
            }

        @Test
        @DisplayName("should return 502 when the source cannot be read")
        fun `should return 502 when the source cannot be read`() =
            testApplication {
                // Given
                coEvery {
                    getAttachmentUseCase.execute(entityId, ImageType.TITLE, null)
                } returns GetAttachmentResult.Unavailable("The source could not be read")
                application {
                    configureDefaultModules()
                    registerControllers()
                }

                // When
                val response = client.get("/v1/attachments?uuid=$entityId&type=TITLE")

                // Then
                assertEquals(HttpStatusCode.BadGateway, response.status)
            }

        @Test
        @DisplayName("should return 400 for a missing or invalid uuid")
        fun `should return 400 for a missing or invalid uuid`() =
            testApplication {
                application {
                    configureDefaultModules()
                    registerControllers()
                }

                // When / Then
                assertEquals(
                    HttpStatusCode.BadRequest,
                    client.get("/v1/attachments?type=THUMBNAIL").status
                )
                assertEquals(
                    HttpStatusCode.BadRequest,
                    client.get("/v1/attachments?uuid=not-a-uuid&type=THUMBNAIL").status
                )
            }

        @Test
        @DisplayName("should return 400 for a missing or invalid type")
        fun `should return 400 for a missing or invalid type`() =
            testApplication {
                application {
                    configureDefaultModules()
                    registerControllers()
                }

                // When / Then
                assertEquals(
                    HttpStatusCode.BadRequest,
                    client.get("/v1/attachments?uuid=$entityId").status
                )
                assertEquals(
                    HttpStatusCode.BadRequest,
                    client.get("/v1/attachments?uuid=$entityId&type=UNKNOWN").status
                )
            }
    }
}
