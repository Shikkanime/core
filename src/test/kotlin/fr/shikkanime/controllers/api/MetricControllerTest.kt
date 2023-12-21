package fr.shikkanime.controllers.api

import fr.shikkanime.module
import io.ktor.client.*
import io.ktor.client.engine.mock.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.server.testing.*
import io.ktor.util.*
import kotlin.test.Test
import kotlin.test.assertEquals

class MetricControllerTest {
    @Test
    fun `get metrics unauthorized`() {
        testApplication {
            application {
                module()
            }

            client.get("/api/metrics").apply {
                assertEquals(HttpStatusCode.Unauthorized, status)
            }
        }
    }

    @OptIn(InternalAPI::class)
    @Test
    fun `get metrics authorized`() {
        testApplication {
            application {
                module()
            }

            val client = HttpClient(MockEngine) {
                engine {
                    addHandler { request ->
                        when (request.url.encodedPath) {
                            "/api/metrics" -> {
                                respond(
                                    content = "[]",
                                    status = HttpStatusCode.OK,
                                    headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                                )
                            }

                            else -> error("Unhandled ${request.url.encodedPath}")
                        }
                    }
                }
            }

            client.get("/api/metrics").apply {
                assertEquals(HttpStatusCode.OK, status)
            }
        }
    }
}