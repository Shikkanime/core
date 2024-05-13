package fr.shikkanime.controllers.api

import com.google.inject.Inject
import fr.shikkanime.dtos.GenericDto
import fr.shikkanime.dtos.MemberDto
import fr.shikkanime.entities.Anime
import fr.shikkanime.entities.EpisodeMapping
import fr.shikkanime.entities.Member
import fr.shikkanime.entities.enums.CountryCode
import fr.shikkanime.entities.enums.EpisodeType
import fr.shikkanime.module
import fr.shikkanime.services.*
import fr.shikkanime.utils.Constant
import fr.shikkanime.utils.MapCache
import fr.shikkanime.utils.ObjectParser
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.testing.*
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
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

    @BeforeEach
    fun setUp() {
        Constant.injector.injectMembers(this)
    }

    @AfterEach
    fun tearDown() {
        memberFollowEpisodeService.deleteAll()
        memberFollowAnimeService.deleteAll()
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

            client.post("/api/v1/members/private-register").apply {
                assertEquals(HttpStatusCode.Created, status)
                val identifier = ObjectParser.fromJson(bodyAsText(), Map::class.java)["identifier"].toString()
                println(identifier)
                val findPrivateMember = memberService.findPrivateMember(identifier)
                assertNotNull(findPrivateMember)
                assertTrue(findPrivateMember!!.isPrivate)
            }
        }
    }

    private suspend fun ApplicationTestBuilder.registerAndLogin(): Pair<String, String> {
        var identifier: String?

        client.post("/api/v1/members/private-register").apply {
            assertEquals(HttpStatusCode.Created, status)
            identifier = ObjectParser.fromJson(bodyAsText(), Map::class.java)["identifier"].toString()
            val findPrivateMember = memberService.findPrivateMember(identifier!!)
            assertNotNull(findPrivateMember)
            assertTrue(findPrivateMember!!.isPrivate)
        }

        client.post("/api/v1/members/private-login") {
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
                val findPrivateMember = memberService.findPrivateMember(identifier)
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
                val findPrivateMember = memberService.findPrivateMember(identifier)
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
                val findPrivateMember = memberService.findPrivateMember(identifier)
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
                val findPrivateMember = memberService.findPrivateMember(identifier)
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
                val findPrivateMember = memberService.findPrivateMember(identifier)
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
                val findPrivateMember = memberService.findPrivateMember(identifier)
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
                val findPrivateMember = memberService.findPrivateMember(identifier)
                val followedEpisodes = memberFollowEpisodeService.findAllFollowedEpisodesUUID(findPrivateMember!!)
                assertNotNull(findPrivateMember)
                assertEquals(12, followedEpisodes.size)
            }
        }
    }
}