package fr.shikkanime.controllers.api

import fr.shikkanime.module
import fr.shikkanime.utils.Constant
import io.ktor.client.*
import io.ktor.client.engine.mock.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.server.testing.*
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

    @Test
    fun `get metrics authorized`() {
        Constant.isDev = true

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
                                    headers = headersOf(
                                        HttpHeaders.ContentType,
                                        ContentType.Application.Json.toString()
                                    )
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