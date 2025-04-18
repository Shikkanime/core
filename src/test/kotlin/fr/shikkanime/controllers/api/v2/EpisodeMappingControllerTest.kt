package fr.shikkanime.controllers.api.v2

import fr.shikkanime.controllers.api.AbstractControllerTest
import fr.shikkanime.dtos.PageableDto
import fr.shikkanime.entities.enums.CountryCode
import fr.shikkanime.module
import fr.shikkanime.utils.ObjectParser
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.testing.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class EpisodeMappingControllerTest : AbstractControllerTest() {
    private val countryCode = CountryCode.FR

    @Test
    fun getAll() {
        testApplication {
            application {
                module()
            }

            client.get("/api/v2/episode-mappings?country=${countryCode}&page=1&limit=4") {
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            }.apply {
                assertEquals(HttpStatusCode.OK, status)
                val episodeMappingsDto = ObjectParser.fromJson(bodyAsText(), PageableDto::class.java)
                assertEquals(4, episodeMappingsDto.data.size)
                assertTrue(episodeMappingsDto.total > 4)
            }

            client.get("/api/v2/episode-mappings?country=${countryCode}&page=2&limit=8") {
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            }.apply {
                assertEquals(HttpStatusCode.OK, status)
                val episodeMappingsDto = ObjectParser.fromJson(bodyAsText(), PageableDto::class.java)
                assertEquals(8, episodeMappingsDto.data.size)
                assertTrue(episodeMappingsDto.total > 8)
            }
        }
    }
}