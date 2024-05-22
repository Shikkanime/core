package fr.shikkanime.controllers.api

import com.google.inject.Inject
import fr.shikkanime.dtos.MemberDto
import fr.shikkanime.module
import fr.shikkanime.services.MemberService
import fr.shikkanime.utils.Constant
import fr.shikkanime.utils.ObjectParser
import fr.shikkanime.utils.StringUtils
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.testing.*
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.*

class MemberActionControllerTest {
    @Inject
    private lateinit var memberService: MemberService

    @BeforeEach
    fun setUp() {
        Constant.injector.injectMembers(this)
    }

    @AfterEach
    fun tearDown() {
        memberService.deleteAll()
    }

    private suspend fun ApplicationTestBuilder.registerAndLogin(): Pair<String, String> {
        var identifier: String?

        client.post("/api/v1/members/register").apply {
            assertEquals(HttpStatusCode.Created, status)
            identifier = ObjectParser.fromJson(bodyAsText(), Map::class.java)["identifier"].toString()
            val findPrivateMember = memberService.findByIdentifier(identifier!!)
            assertNotNull(findPrivateMember)
            assertTrue(findPrivateMember!!.isPrivate)
        }

        client.post("/api/v1/members/login") {
            setBody(identifier!!)
        }.apply {
            assertEquals(HttpStatusCode.OK, status)
            val tokenDto = ObjectParser.fromJson(bodyAsText(), MemberDto::class.java)
            assertTrue(tokenDto.token.isNotBlank())
            return identifier!! to tokenDto.token
        }
    }

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
                setBody("")
            }.apply {
                assertEquals(HttpStatusCode.BadRequest, status)
            }
        }
    }
}