package fr.shikkanime.controllers.api

import fr.shikkanime.module
import fr.shikkanime.utils.StringUtils
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.server.testing.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.util.*

class MemberActionControllerTest : AbstractControllerTest() {
    @Test
    fun `no uuid action`() {
        testApplication {
            application {
                module()
            }

            val (_, token) = registerAndLogin()

            client.post("/api/v1/member-actions/validate") {
                header(HttpHeaders.Authorization, "Bearer $token")
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(StringUtils.generateRandomString(8))
            }.apply {
                assertEquals(HttpStatusCode.BadRequest, status)
            }
        }
    }

    @Test
    fun `blank action`() {
        testApplication {
            application {
                module()
            }

            val (_, token) = registerAndLogin()

            client.post("/api/v1/member-actions/validate?uuid=${UUID.randomUUID()}") {
                header(HttpHeaders.Authorization, "Bearer $token")
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(StringUtils.EMPTY_STRING)
            }.apply {
                assertEquals(HttpStatusCode.BadRequest, status)
            }
        }
    }
}