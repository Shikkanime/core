package fr.shikkanime.controllers.api

import fr.shikkanime.dtos.GenericDto
import fr.shikkanime.dtos.member.MemberDto
import fr.shikkanime.dtos.member.RefreshMemberDto
import fr.shikkanime.entities.Member
import fr.shikkanime.entities.MemberAction
import fr.shikkanime.entities.enums.Action
import fr.shikkanime.entities.enums.ImageType
import fr.shikkanime.module
import fr.shikkanime.utils.FileManager
import fr.shikkanime.utils.MapCache
import fr.shikkanime.utils.ObjectParser
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.testing.*
import io.ktor.utils.io.jvm.javaio.*
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.io.ByteArrayOutputStream
import java.util.*

class MemberControllerTest : AbstractControllerTest() {
    @Test
    fun registerPrivateMember() {
        testApplication {
            application {
                module()
            }

            client.post("/api/v1/members/register").apply {
                assertEquals(HttpStatusCode.Created, status)
                val identifier = ObjectParser.fromJson(bodyAsText(), Map::class.java)["identifier"].toString()
                val findPrivateMember = memberService.findByIdentifier(identifier)
                assertNotNull(findPrivateMember)
                assertTrue(findPrivateMember!!.isPrivate)
            }
        }
    }

    @Test
    fun loginPrivateMember() {
        testApplication {
            application {
                module()
            }

            registerAndLogin()
        }
    }

    @Test
    fun `try to login with admin`() {
        testApplication {
            application {
                module()
            }

            client.post("/api/v1/members/private-login") {
                setBody("admin")
            }.apply {
                assertEquals(HttpStatusCode.NotFound, status)
            }
        }
    }

    @Test
    fun associateEmail() {
        testApplication {
            application {
                module()
            }

            val (identifier, token) = registerAndLogin()
            val findPrivateMember = memberService.findByIdentifier(identifier)
            var memberAction: MemberAction?

            client.post("/api/v1/members/associate-email") {
                header(HttpHeaders.Authorization, "Bearer $token")
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody("contact@shikkanime.fr")
            }.apply {
                assertEquals(HttpStatusCode.Created, status)
                val dto = ObjectParser.fromJson(bodyAsText(), GenericDto::class.java)
                memberAction = memberActionService.find(dto.uuid)
                assertNotNull(memberAction)
                assertEquals(findPrivateMember!!.uuid, memberAction!!.member!!.uuid)
                assertEquals(memberAction.email, "contact@shikkanime.fr")
                assertEquals(memberAction.action, Action.VALIDATE_EMAIL)
                assertEquals(memberAction.validated, false)
                assertNotNull(memberAction.code)
            }

            client.post("/api/v1/member-actions/validate?uuid=${memberAction!!.uuid}") {
                header(HttpHeaders.Authorization, "Bearer $token")
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(memberAction!!.code!!)
            }.apply {
                assertEquals(HttpStatusCode.OK, status)
                memberAction = memberActionService.find(memberAction.uuid)
                val member = memberService.findByIdentifier(identifier)
                assertNotNull(memberAction)
                assertEquals(findPrivateMember!!.uuid, memberAction!!.member!!.uuid)
                assertEquals(memberAction.email, "contact@shikkanime.fr")
                assertEquals(memberAction.action, Action.VALIDATE_EMAIL)
                assertEquals(memberAction.validated, true)
                assertNotNull(memberAction.code)
                assertEquals(member!!.email, "contact@shikkanime.fr")
            }
        }
    }

    @Test
    fun associateEmailAndLogin() {
        testApplication {
            application {
                module()
            }

            val (identifier, token) = registerAndLogin()
            val findPrivateMember = memberService.findByIdentifier(identifier)
            var memberAction: MemberAction?

            client.post("/api/v1/members/associate-email") {
                header(HttpHeaders.Authorization, "Bearer $token")
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody("contact@shikkanime.fr")
            }.apply {
                assertEquals(HttpStatusCode.Created, status)
                val dto = ObjectParser.fromJson(bodyAsText(), GenericDto::class.java)
                memberAction = memberActionService.find(dto.uuid)
                assertNotNull(memberAction)
                assertEquals(findPrivateMember!!.uuid, memberAction!!.member!!.uuid)
                assertEquals(memberAction.email, "contact@shikkanime.fr")
                assertEquals(memberAction.action, Action.VALIDATE_EMAIL)
                assertEquals(memberAction.validated, false)
                assertNotNull(memberAction.code)
            }

            client.post("/api/v1/member-actions/validate?uuid=${memberAction!!.uuid}") {
                header(HttpHeaders.Authorization, "Bearer $token")
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(memberAction!!.code!!)
            }.apply {
                assertEquals(HttpStatusCode.OK, status)
                memberAction = memberActionService.find(memberAction.uuid)
                val member = memberService.findByIdentifier(identifier)
                assertNotNull(memberAction)
                assertEquals(findPrivateMember!!.uuid, memberAction!!.member!!.uuid)
                assertEquals(memberAction.email, "contact@shikkanime.fr")
                assertEquals(memberAction.action, Action.VALIDATE_EMAIL)
                assertEquals(memberAction.validated, true)
                assertNotNull(memberAction.code)
                assertEquals(member!!.email, "contact@shikkanime.fr")
            }

            client.post("/api/v1/members/login") {
                setBody(identifier)
            }.apply {
                assertEquals(HttpStatusCode.OK, status)
                val tokenDto = ObjectParser.fromJson(bodyAsText(), MemberDto::class.java)
                assertTrue(tokenDto.token.isNotBlank())
            }
        }
    }

    @Test
    fun forgotIdentifier() {
        testApplication {
            application {
                module()
            }

            val (_, token1) = registerAndLogin()
            val (identifier2, _) = registerAndLogin()
            val findPrivateMember2 = memberService.findByIdentifier(identifier2)!!
            findPrivateMember2.email = "contact@shikkanime.fr"
            memberService.update(findPrivateMember2)
            MapCache.invalidate(Member::class.java)

            var memberAction: MemberAction?

            client.post("/api/v1/members/forgot-identifier") {
                header(HttpHeaders.Authorization, "Bearer $token1")
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody("contact@shikkanime.fr")
            }.apply {
                assertEquals(HttpStatusCode.Created, status)
                val dto = ObjectParser.fromJson(bodyAsText(), GenericDto::class.java)
                memberAction = memberActionService.find(dto.uuid)
                assertNotNull(memberAction)
                assertEquals(findPrivateMember2.uuid, memberAction!!.member!!.uuid)
                assertEquals(memberAction.email, "contact@shikkanime.fr")
                assertEquals(memberAction.action, Action.FORGOT_IDENTIFIER)
                assertEquals(memberAction.validated, false)
                assertNotNull(memberAction.code)
            }

            client.post("/api/v1/member-actions/validate?uuid=${memberAction!!.uuid}") {
                header(HttpHeaders.Authorization, "Bearer $token1")
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(memberAction!!.code!!)
            }.apply {
                assertEquals(HttpStatusCode.OK, status)
                memberAction = memberActionService.find(memberAction.uuid)
                val member = memberService.findByIdentifier(identifier2)
                assertNotNull(memberAction)
                assertNull(member)

                assertEquals(findPrivateMember2.uuid, memberAction!!.member!!.uuid)
                assertEquals(memberAction.email, "contact@shikkanime.fr")
                assertEquals(memberAction.action, Action.FORGOT_IDENTIFIER)
                assertEquals(memberAction.validated, true)
                assertNotNull(memberAction.code)

                assertNotEquals(memberAction.member!!.username, findPrivateMember2.username)
            }
        }
    }

    @Test
    fun followAnime() {
        testApplication {
            application {
                module()
            }

            val (identifier, token) = registerAndLogin()
            val anime = animeService.findAll().first()

            client.put("/api/v1/members/animes") {
                header(HttpHeaders.Authorization, "Bearer $token")
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(ObjectParser.toJson(GenericDto(anime.uuid!!)))
            }.apply {
                assertEquals(HttpStatusCode.OK, status)
                val findPrivateMember = memberService.findByIdentifier(identifier)
                val followedAnimesUUID = memberFollowAnimeService.findAllFollowedAnimesUUID(findPrivateMember!!.uuid!!)
                assertNotNull(findPrivateMember)
                assertEquals(1, followedAnimesUUID.size)
                assertEquals(anime.uuid, followedAnimesUUID.first())
            }
        }
    }

    @Test
    fun `follow a random anime`() {
        testApplication {
            application {
                module()
            }

            val (_, token) = registerAndLogin()

            client.put("/api/v1/members/animes") {
                header(HttpHeaders.Authorization, "Bearer $token")
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(ObjectParser.toJson(GenericDto(UUID.randomUUID())))
            }.apply {
                assertEquals(HttpStatusCode.NotFound, status)
            }
        }
    }

    @Test
    fun unfollowAnime() {
        testApplication {
            application {
                module()
            }

            val (identifier, token) = registerAndLogin()
            val anime = animeService.findAll().first()

            client.put("/api/v1/members/animes") {
                header(HttpHeaders.Authorization, "Bearer $token")
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(ObjectParser.toJson(GenericDto(anime.uuid!!)))
            }.apply {
                assertEquals(HttpStatusCode.OK, status)
                val findPrivateMember = memberService.findByIdentifier(identifier)
                val followedAnimesUUID = memberFollowAnimeService.findAllFollowedAnimesUUID(findPrivateMember!!.uuid!!)
                assertNotNull(findPrivateMember)
                assertEquals(1, followedAnimesUUID.size)
                assertEquals(anime.uuid, followedAnimesUUID.first())
            }

            client.delete("/api/v1/members/animes") {
                header(HttpHeaders.Authorization, "Bearer $token")
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(ObjectParser.toJson(GenericDto(anime.uuid!!)))
            }.apply {
                assertEquals(HttpStatusCode.OK, status)
                val findPrivateMember = memberService.findByIdentifier(identifier)
                val followedAnimesUUID = memberFollowAnimeService.findAllFollowedAnimesUUID(findPrivateMember!!.uuid!!)
                assertNotNull(findPrivateMember)
                assertEquals(0, followedAnimesUUID.size)
            }
        }
    }

    @Test
    fun followEpisode() {
        testApplication {
            application {
                module()
            }

            val (identifier, token) = registerAndLogin()
            val anime = animeService.findAll().first()
            val episode = episodeMappingService.findAllByAnime(anime).first()

            client.put("/api/v1/members/episodes") {
                header(HttpHeaders.Authorization, "Bearer $token")
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(ObjectParser.toJson(GenericDto(episode.uuid!!)))
            }.apply {
                assertEquals(HttpStatusCode.OK, status)
                val findPrivateMember = memberService.findByIdentifier(identifier)
                val followedEpisodesUUID = memberFollowEpisodeService.findAllFollowedEpisodesUUID(findPrivateMember!!.uuid!!)
                assertNotNull(findPrivateMember)
                assertEquals(1, followedEpisodesUUID.size)
                assertEquals(episode.uuid, followedEpisodesUUID.first())
            }
        }
    }

    @Test
    fun `follow a random episode`() {
        testApplication {
            application {
                module()
            }

            val (_, token) = registerAndLogin()

            client.put("/api/v1/members/episodes") {
                header(HttpHeaders.Authorization, "Bearer $token")
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(ObjectParser.toJson(GenericDto(UUID.randomUUID())))
            }.apply {
                assertEquals(HttpStatusCode.NotFound, status)
            }
        }
    }

    @Test
    fun unfollowEpisode() {
        testApplication {
            application {
                module()
            }

            val (identifier, token) = registerAndLogin()
            val anime = animeService.findAll().first()
            val episode = episodeMappingService.findAllByAnime(anime).first()

            client.put("/api/v1/members/episodes") {
                header(HttpHeaders.Authorization, "Bearer $token")
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(ObjectParser.toJson(GenericDto(episode.uuid!!)))
            }.apply {
                assertEquals(HttpStatusCode.OK, status)
                val findPrivateMember = memberService.findByIdentifier(identifier)
                val followedEpisodesUUID = memberFollowEpisodeService.findAllFollowedEpisodesUUID(findPrivateMember!!.uuid!!)
                assertNotNull(findPrivateMember)
                assertEquals(1, followedEpisodesUUID.size)
                assertEquals(episode.uuid, followedEpisodesUUID.first())
            }

            client.delete("/api/v1/members/episodes") {
                header(HttpHeaders.Authorization, "Bearer $token")
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(ObjectParser.toJson(GenericDto(episode.uuid!!)))
            }.apply {
                assertEquals(HttpStatusCode.OK, status)
                val findPrivateMember = memberService.findByIdentifier(identifier)
                val followedEpisodes = memberFollowEpisodeService.findAllFollowedEpisodesUUID(findPrivateMember!!.uuid!!)
                assertNotNull(findPrivateMember)
                assertEquals(0, followedEpisodes.size)
            }
        }
    }

    @Test
    fun followAllEpisodes() {
        testApplication {
            application {
                module()
            }

            val (identifier, token) = registerAndLogin()
            val anime = animeService.findAll().first()

            client.put("/api/v1/members/follow-all-episodes") {
                header(HttpHeaders.Authorization, "Bearer $token")
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(ObjectParser.toJson(GenericDto(anime.uuid!!)))
            }.apply {
                assertEquals(HttpStatusCode.OK, status)
                val findPrivateMember = memberService.findByIdentifier(identifier)
                val followedEpisodes = memberFollowEpisodeService.findAllFollowedEpisodesUUID(findPrivateMember!!.uuid!!)
                assertNotNull(findPrivateMember)
                assertEquals(116, followedEpisodes.size)
            }
        }
    }

    @Test
    fun uploadProfileImage() {
        testApplication {
            application {
                module()
            }

            val fromResource = FileManager.getInputStreamFromResource("avatar.jpg")
            val (identifier, token) = registerAndLogin()
            val member = memberService.findByIdentifier(identifier)

            client.submitFormWithBinaryData(
                "/api/v1/members/image",
                formData {
                    append("file", fromResource.readBytes(), Headers.build {
                        append(HttpHeaders.ContentType, ContentType.Image.JPEG.toString())
                        append(HttpHeaders.ContentDisposition, "filename=avatar.jpg")
                    })
                }
            ) {
                header(HttpHeaders.Authorization, "Bearer $token")
            }.apply {
                assertEquals(HttpStatusCode.OK, status)
            }

            client.get("/api/v1/attachments?uuid=${member!!.uuid}&type=${ImageType.MEMBER_PROFILE}").apply {
                assertEquals(HttpStatusCode.OK, status)
                val byteArrayOutputStream = ByteArrayOutputStream()
                runBlocking { bodyAsChannel().copyTo(byteArrayOutputStream) }
                val image = byteArrayOutputStream.toByteArray()
                assertTrue(image.isNotEmpty())
            }
        }
    }

    @Test
    fun refreshMember() {
        testApplication {
            application {
                module()
            }

            val (identifier, token) = registerAndLogin()
            val anime = animeService.findAll().first()

            client.put("/api/v1/members/animes") {
                header(HttpHeaders.Authorization, "Bearer $token")
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(ObjectParser.toJson(GenericDto(anime.uuid!!)))
            }.apply {
                assertEquals(HttpStatusCode.OK, status)
                val findPrivateMember = memberService.findByIdentifier(identifier)
                val followedAnimesUUID = memberFollowAnimeService.findAllFollowedAnimesUUID(findPrivateMember!!.uuid!!)
                assertNotNull(findPrivateMember)
                assertEquals(1, followedAnimesUUID.size)
                assertEquals(anime.uuid, followedAnimesUUID.first())
            }

            client.put("/api/v1/members/follow-all-episodes") {
                header(HttpHeaders.Authorization, "Bearer $token")
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(ObjectParser.toJson(GenericDto(anime.uuid!!)))
            }.apply {
                assertEquals(HttpStatusCode.OK, status)
                val findPrivateMember = memberService.findByIdentifier(identifier)
                val followedEpisodes = memberFollowEpisodeService.findAllFollowedEpisodesUUID(findPrivateMember!!.uuid!!)
                assertNotNull(findPrivateMember)
                assertEquals(116, followedEpisodes.size)
            }

            client.get("/api/v1/members/refresh") {
                header(HttpHeaders.Authorization, "Bearer $token")
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            }.apply {
                assertEquals(HttpStatusCode.OK, status)
                val refreshMemberDto = ObjectParser.fromJson(bodyAsText(), RefreshMemberDto::class.java)
                println(refreshMemberDto)
                val findPrivateMember = memberService.findByIdentifier(identifier)
                val followedEpisodes = memberFollowEpisodeService.findAllFollowedEpisodesUUID(findPrivateMember!!.uuid!!)
                assertNotNull(findPrivateMember)
                assertEquals(1, refreshMemberDto.followedAnimes.total)
                assertEquals(followedEpisodes.size.toLong(), refreshMemberDto.followedEpisodes.total)
                assertEquals(9, refreshMemberDto.followedEpisodes.data.size)
                assertTrue(refreshMemberDto.totalDuration > 0)
                assertEquals(0, refreshMemberDto.totalUnseenDuration)
            }
        }
    }

    @Test
    fun refreshMemberWithLimit() {
        testApplication {
            application {
                module()
            }

            val (identifier, token) = registerAndLogin()
            val anime = animeService.findAll().first()

            client.put("/api/v1/members/animes") {
                header(HttpHeaders.Authorization, "Bearer $token")
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(ObjectParser.toJson(GenericDto(anime.uuid!!)))
            }.apply {
                assertEquals(HttpStatusCode.OK, status)
                val findPrivateMember = memberService.findByIdentifier(identifier)
                val followedAnimesUUID = memberFollowAnimeService.findAllFollowedAnimesUUID(findPrivateMember!!.uuid!!)
                assertNotNull(findPrivateMember)
                assertEquals(1, followedAnimesUUID.size)
                assertEquals(anime.uuid, followedAnimesUUID.first())
            }

            client.put("/api/v1/members/follow-all-episodes") {
                header(HttpHeaders.Authorization, "Bearer $token")
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(ObjectParser.toJson(GenericDto(anime.uuid!!)))
            }.apply {
                assertEquals(HttpStatusCode.OK, status)
                val findPrivateMember = memberService.findByIdentifier(identifier)
                val followedEpisodes = memberFollowEpisodeService.findAllFollowedEpisodesUUID(findPrivateMember!!.uuid!!)
                assertNotNull(findPrivateMember)
                assertEquals(116, followedEpisodes.size)
            }

            client.get("/api/v1/members/refresh?limit=3") {
                header(HttpHeaders.Authorization, "Bearer $token")
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            }.apply {
                assertEquals(HttpStatusCode.OK, status)
                val refreshMemberDto = ObjectParser.fromJson(bodyAsText(), RefreshMemberDto::class.java)
                println(refreshMemberDto)
                val findPrivateMember = memberService.findByIdentifier(identifier)
                val followedEpisodes = memberFollowEpisodeService.findAllFollowedEpisodesUUID(findPrivateMember!!.uuid!!)
                assertNotNull(findPrivateMember)
                assertEquals(1, refreshMemberDto.followedAnimes.total)
                assertEquals(followedEpisodes.size.toLong(), refreshMemberDto.followedEpisodes.total)
                assertEquals(3, refreshMemberDto.followedEpisodes.data.size)
                assertTrue(refreshMemberDto.totalDuration > 0)
                assertEquals(0, refreshMemberDto.totalUnseenDuration)
            }
        }
    }
}