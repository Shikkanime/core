package fr.shikkanime.controllers.api

import fr.shikkanime.dtos.SimulcastDto
import fr.shikkanime.module
import fr.shikkanime.utils.ObjectParser
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.testing.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SimulcastControllerTest : AbstractControllerTest() {
    @Test
    fun getAll() {
        testApplication {
            application {
                module()
            }

            client.get("/api/v1/simulcasts") {
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            }.apply {
                assertEquals(HttpStatusCode.OK, status)
                val simulcastsDto = ObjectParser.fromJson(bodyAsText(), Array<SimulcastDto>::class.java)
                assertTrue(simulcastsDto.isNotEmpty())
            }
        }
    }
}