package fr.shikkanime.controllers.api

import com.google.inject.Inject
import fr.shikkanime.dtos.GenericDto
import fr.shikkanime.dtos.MemberDto
import fr.shikkanime.entities.Anime
import fr.shikkanime.entities.EpisodeMapping
import fr.shikkanime.entities.Member
import fr.shikkanime.entities.MemberAction
import fr.shikkanime.entities.enums.Action
import fr.shikkanime.entities.enums.CountryCode
import fr.shikkanime.entities.enums.EpisodeType
import fr.shikkanime.module
import fr.shikkanime.services.*
import fr.shikkanime.utils.Constant
import fr.shikkanime.utils.FileManager
import fr.shikkanime.utils.MapCache
import fr.shikkanime.utils.ObjectParser
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.testing.*
import io.ktor.utils.io.jvm.javaio.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.ByteArrayOutputStream
import java.util.*

class MemberControllerTest {
    @Inject
    private lateinit var memberService: MemberService

    @Inject
    private lateinit var animeService: AnimeService

    @Inject
    private lateinit var episodeMappingService: EpisodeMappingService

    @Inject
    private lateinit var memberFollowAnimeService: MemberFollowAnimeService

    @Inject
    private lateinit var memberFollowEpisodeService: MemberFollowEpisodeService

    @Inject
    private lateinit var memberActionService: MemberActionService

    @BeforeEach
    fun setUp() {
        Constant.injector.injectMembers(this)
    }

    @AfterEach
    fun tearDown() {
        memberFollowEpisodeService.deleteAll()
        memberFollowAnimeService.deleteAll()
        memberActionService.deleteAll()
        memberService.deleteAll()
        episodeMappingService.deleteAll()
        animeService.deleteAll()
        MapCache.invalidate(Member::class.java)
    }

    @Test
    fun registerPrivateMember() {
        testApplication {
            application {
                module()
            }

            client.post("/api/v1/members/register").apply {
                assertEquals(HttpStatusCode.Created, status)
                val identifier = ObjectParser.fromJson(bodyAsText(), Map::class.java)["identifier"].toString()
                println(identifier)
                val findPrivateMember = memberService.findByIdentifier(identifier)
                assertNotNull(findPrivateMember)
                assertTrue(findPrivateMember!!.isPrivate)
            }
        }
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
                assertEquals(memberAction!!.email, "contact@shikkanime.fr")
                assertEquals(memberAction!!.action, Action.VALIDATE_EMAIL)
                assertEquals(memberAction!!.validated, false)
                assertNotNull(memberAction!!.code)
            }

            client.post("/api/v1/member-actions/validate?uuid=${memberAction!!.uuid}") {
                header(HttpHeaders.Authorization, "Bearer $token")
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(memberAction!!.code!!)
            }.apply {
                assertEquals(HttpStatusCode.OK, status)
                memberAction = memberActionService.find(memberAction!!.uuid)
                val member = memberService.findByIdentifier(identifier)
                assertNotNull(memberAction)
                assertEquals(findPrivateMember!!.uuid, memberAction!!.member!!.uuid)
                assertEquals(memberAction!!.email, "contact@shikkanime.fr")
                assertEquals(memberAction!!.action, Action.VALIDATE_EMAIL)
                assertEquals(memberAction!!.validated, true)
                assertNotNull(memberAction!!.code)
                assertEquals(member!!.email, "contact@shikkanime.fr")
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

            val anime = animeService.save(
                Anime(
                    countryCode = CountryCode.FR,
                    name = "Test Anime",
                    image = "test.jpg",
                    slug = "test-anime",
                    banner = "test-banner.jpg",
                    description = "Test description",
                )
            )

            client.put("/api/v1/members/animes") {
                header(HttpHeaders.Authorization, "Bearer $token")
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(ObjectParser.toJson(GenericDto(anime.uuid!!)))
            }.apply {
                assertEquals(HttpStatusCode.OK, status)
                val findPrivateMember = memberService.findByIdentifier(identifier)
                val followedAnimesUUID = memberFollowAnimeService.findAllFollowedAnimesUUID(findPrivateMember!!)
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

            val anime = animeService.save(
                Anime(
                    countryCode = CountryCode.FR,
                    name = "Test Anime",
                    image = "test.jpg",
                    slug = "test-anime",
                    banner = "test-banner.jpg",
                    description = "Test description",
                )
            )

            client.put("/api/v1/members/animes") {
                header(HttpHeaders.Authorization, "Bearer $token")
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(ObjectParser.toJson(GenericDto(anime.uuid!!)))
            }.apply {
                assertEquals(HttpStatusCode.OK, status)
                val findPrivateMember = memberService.findByIdentifier(identifier)
                val followedAnimesUUID = memberFollowAnimeService.findAllFollowedAnimesUUID(findPrivateMember!!)
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
                val followedAnimesUUID = memberFollowAnimeService.findAllFollowedAnimesUUID(findPrivateMember!!)
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

            val anime = animeService.save(
                Anime(
                    countryCode = CountryCode.FR,
                    name = "Test Anime",
                    image = "test.jpg",
                    slug = "test-anime",
                    banner = "test-banner.jpg",
                    description = "Test description",
                )
            )

            val episode = episodeMappingService.save(
                EpisodeMapping(
                    anime = anime,
                    episodeType = EpisodeType.FILM,
                    season = 1,
                    number = 1,
                    image = "test.jpg",
                )
            )

            client.put("/api/v1/members/episodes") {
                header(HttpHeaders.Authorization, "Bearer $token")
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(ObjectParser.toJson(GenericDto(episode.uuid!!)))
            }.apply {
                assertEquals(HttpStatusCode.OK, status)
                val findPrivateMember = memberService.findByIdentifier(identifier)
                val followedEpisodesUUID = memberFollowEpisodeService.findAllFollowedEpisodesUUID(findPrivateMember!!)
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

            val anime = animeService.save(
                Anime(
                    countryCode = CountryCode.FR,
                    name = "Test Anime",
                    image = "test.jpg",
                    slug = "test-anime",
                    banner = "test-banner.jpg",
                    description = "Test description",
                )
            )

            val episode = episodeMappingService.save(
                EpisodeMapping(
                    anime = anime,
                    episodeType = EpisodeType.FILM,
                    season = 1,
                    number = 1,
                    image = "test.jpg",
                )
            )

            client.put("/api/v1/members/episodes") {
                header(HttpHeaders.Authorization, "Bearer $token")
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(ObjectParser.toJson(GenericDto(episode.uuid!!)))
            }.apply {
                assertEquals(HttpStatusCode.OK, status)
                val findPrivateMember = memberService.findByIdentifier(identifier)
                val followedEpisodesUUID = memberFollowEpisodeService.findAllFollowedEpisodesUUID(findPrivateMember!!)
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
                val followedEpisodes = memberFollowEpisodeService.findAllFollowedEpisodesUUID(findPrivateMember!!)
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

            val anime = animeService.save(
                Anime(
                    countryCode = CountryCode.FR,
                    name = "Test Anime",
                    image = "test.jpg",
                    slug = "test-anime",
                    banner = "test-banner.jpg",
                    description = "Test description",
                )
            )

            (1..12).forEach {
                episodeMappingService.save(
                    EpisodeMapping(
                        anime = anime,
                        episodeType = EpisodeType.EPISODE,
                        season = 1,
                        number = it,
                        image = "test.jpg",
                    )
                )
            }

            client.put("/api/v1/members/follow-all-episodes") {
                header(HttpHeaders.Authorization, "Bearer $token")
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(ObjectParser.toJson(GenericDto(anime.uuid!!)))
            }.apply {
                assertEquals(HttpStatusCode.OK, status)
                val findPrivateMember = memberService.findByIdentifier(identifier)
                val followedEpisodes = memberFollowEpisodeService.findAllFollowedEpisodesUUID(findPrivateMember!!)
                assertNotNull(findPrivateMember)
                assertEquals(12, followedEpisodes.size)
            }
        }
    }

    @Test
    fun uploadProfileImage() {
        testApplication {
            application {
                module()
            }

            val (identifier, token) = registerAndLogin()
            val member = memberService.findByIdentifier(identifier)

            client.post("/api/v1/members/image") {
                header(HttpHeaders.Authorization, "Bearer $token")
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(FileManager.getInputStreamFromResource("avatar.jpg").readBytes())
            }.apply {
                assertEquals(HttpStatusCode.OK, status)
            }

            delay(1000)

            client.get("/api/v1/attachments?uuid=${member!!.uuid}").apply {
                assertEquals(HttpStatusCode.OK, status)
                val byteArrayOutputStream = ByteArrayOutputStream()
                runBlocking { bodyAsChannel().copyTo(byteArrayOutputStream) }
                val image = byteArrayOutputStream.toByteArray()
                assertTrue(image.isNotEmpty())
            }
        }
    }
}