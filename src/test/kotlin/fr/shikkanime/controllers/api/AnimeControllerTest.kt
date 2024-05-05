package fr.shikkanime.controllers.api

import com.google.inject.Inject
import fr.shikkanime.dtos.GenericDto
import fr.shikkanime.dtos.MemberDto
import fr.shikkanime.dtos.WeeklyAnimesDto
import fr.shikkanime.entities.Anime
import fr.shikkanime.entities.EpisodeMapping
import fr.shikkanime.entities.EpisodeVariant
import fr.shikkanime.entities.enums.CountryCode
import fr.shikkanime.entities.enums.EpisodeType
import fr.shikkanime.entities.enums.Platform
import fr.shikkanime.module
import fr.shikkanime.services.*
import fr.shikkanime.utils.Constant
import fr.shikkanime.utils.ObjectParser
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.testing.*
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class AnimeControllerTest {
    @Inject
    private lateinit var memberService: MemberService

    @Inject
    private lateinit var animeService: AnimeService

    @Inject
    private lateinit var episodeMappingService: EpisodeMappingService

    @Inject
    private lateinit var episodeVariantService: EpisodeVariantService

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
        episodeVariantService.deleteAll()
        episodeMappingService.deleteAll()
        animeService.deleteAll()
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
    fun getWeekly() {
        testApplication {
            application {
                module()
            }

            val (identifier, token) = registerAndLogin()

            val anime1 = animeService.save(
                Anime(
                    countryCode = CountryCode.FR,
                    name = "Test Anime 1",
                    image = "test.jpg",
                    slug = "test-anime-1",
                    banner = "test-banner.jpg",
                    description = "Test description",
                )
            )

            episodeMappingService.save(
                EpisodeMapping(
                    anime = anime1,
                    episodeType = EpisodeType.EPISODE,
                    season = 1,
                    number = 1,
                    image = "test.jpg",
                )
            ).apply {
                episodeVariantService.save(
                    EpisodeVariant(
                        mapping = this,
                        platform = Platform.CRUN,
                        audioLocale = "ja-JP",
                        identifier = "test-episode-1",
                        url = "test.mp4",
                        uncensored = true
                    )
                )
            }

            val anime2 = animeService.save(
                Anime(
                    countryCode = CountryCode.FR,
                    name = "Test Anime 2",
                    image = "test.jpg",
                    slug = "test-anime-2",
                    banner = "test-banner.jpg",
                    description = "Test description",
                )
            )

            episodeMappingService.save(
                EpisodeMapping(
                    anime = anime2,
                    episodeType = EpisodeType.EPISODE,
                    season = 1,
                    number = 1,
                    image = "test.jpg",
                )
            ).apply {
                episodeVariantService.save(
                    EpisodeVariant(
                        mapping = this,
                        platform = Platform.CRUN,
                        audioLocale = "ja-JP",
                        identifier = "test-episode-2",
                        url = "test.mp4",
                        uncensored = true
                    )
                )
            }

            client.put("/api/v1/members/animes") {
                header(HttpHeaders.Authorization, "Bearer $token")
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(ObjectParser.toJson(GenericDto(anime1.uuid!!)))
            }.apply {
                assertEquals(HttpStatusCode.OK, status)
                val findPrivateMember = memberService.findPrivateMember(identifier)
                val followedAnimes = memberFollowAnimeService.getAllFollowedAnimes(findPrivateMember!!)
                assertNotNull(findPrivateMember)
                assertEquals(1, followedAnimes.size)
                assertEquals(anime1.uuid, followedAnimes.first().uuid)
            }

            client.get("/api/v1/animes/weekly") {
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            }.apply {
                assertEquals(HttpStatusCode.OK, status)
                val weeklyAnimesDto =
                    ObjectParser.fromJson(bodyAsText(), Array<WeeklyAnimesDto>::class.java).flatMap { it.releases }
                assertEquals(2, weeklyAnimesDto.size)
            }

            client.get("/api/v1/animes/member-weekly") {
                header(HttpHeaders.Authorization, "Bearer $token")
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            }.apply {
                assertEquals(HttpStatusCode.OK, status)
                val weeklyAnimesDto =
                    ObjectParser.fromJson(bodyAsText(), Array<WeeklyAnimesDto>::class.java).flatMap { it.releases }
                assertEquals(1, weeklyAnimesDto.size)
                assertEquals(anime1.uuid, weeklyAnimesDto.first().anime.uuid)
            }
        }
    }
}