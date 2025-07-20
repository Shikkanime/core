package fr.shikkanime.controllers.api

import com.google.gson.reflect.TypeToken
import fr.shikkanime.dtos.GenericDto
import fr.shikkanime.dtos.PageableDto
import fr.shikkanime.dtos.animes.AnimeDto
import fr.shikkanime.dtos.animes.MissedAnimeDto
import fr.shikkanime.dtos.weekly.WeeklyAnimesDto
import fr.shikkanime.entities.enums.CountryCode
import fr.shikkanime.entities.enums.LangType
import fr.shikkanime.module
import fr.shikkanime.utils.ObjectParser
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.testing.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

class AnimeControllerTest : AbstractControllerTest() {
    @Test
    fun getWeekly() {
        testApplication {
            application {
                module()
            }

            val (identifier, token) = registerAndLogin()
            val animes = animeService.findAll()
            val anime = animes.first()

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

            client.get("/api/v1/animes/weekly?date=2024-01-01") {
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            }.apply {
                assertEquals(HttpStatusCode.OK, status)
                val weeklyAnimesDto =
                    ObjectParser.fromJson(bodyAsText(), Array<WeeklyAnimesDto>::class.java)
                        .flatMap { it.releases }
                assertEquals(2, weeklyAnimesDto.size)
                assertTrue(weeklyAnimesDto.all { !it.anime.seasons.isNullOrEmpty() })
            }

            client.get("/api/v1/animes/weekly?date=2024-01-01") {
                header(HttpHeaders.Authorization, "Bearer $token")
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            }.apply {
                assertEquals(HttpStatusCode.OK, status)
                val weeklyAnimesDto =
                    ObjectParser.fromJson(bodyAsText(), Array<WeeklyAnimesDto>::class.java)
                        .flatMap { it.releases }
                assertEquals(1, weeklyAnimesDto.size)
                assertEquals(anime.uuid, weeklyAnimesDto.first().anime.uuid)
                assertTrue(weeklyAnimesDto.all { !it.anime.seasons.isNullOrEmpty() })
            }
        }
    }

    @Test
    fun getMissedAnimes() {
        testApplication {
            application {
                module()
            }

            val (identifier, token) = registerAndLogin()
            val anime = animeService.findAll().first()
            val episodes = episodeMappingService.findAllByAnime(anime)

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

            client.put("/api/v1/members/episodes") {
                header(HttpHeaders.Authorization, "Bearer $token")
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(ObjectParser.toJson(GenericDto(episodes.first().uuid!!)))
            }.apply {
                assertEquals(HttpStatusCode.OK, status)
                val findPrivateMember = memberService.findByIdentifier(identifier)
                val followedEpisodesUUID = memberFollowEpisodeService.findAllFollowedEpisodesUUID(findPrivateMember!!.uuid!!)
                assertNotNull(findPrivateMember)
                assertEquals(1, followedEpisodesUUID.size)
                assertEquals(episodes.first().uuid, followedEpisodesUUID.first())
            }

            client.get("/api/v1/animes/missed?page=1&limit=8") {
                header(HttpHeaders.Authorization, "Bearer $token")
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            }.apply {
                assertEquals(HttpStatusCode.OK, status)
                val missedAnimes =
                    ObjectParser.fromJson(bodyAsText(), object : TypeToken<PageableDto<MissedAnimeDto>>() {})
                assertEquals(1, missedAnimes.data.size)
                assertEquals(115, missedAnimes.data.first().episodeMissed)
            }
        }
    }

    @Test
    fun advancedSearch() {
        testApplication {
            application {
                module()
            }

            client.get(
                "/api/v1/animes?country=${CountryCode.FR}&name=${
                    URLEncoder.encode(
                        "#",
                        StandardCharsets.UTF_8
                    )
                }&searchTypes=${LangType.entries.joinToString(",")}"
            ) {
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            }.apply {
                assertEquals(HttpStatusCode.OK, status)
                val animes = ObjectParser.fromJson(bodyAsText(), object : TypeToken<PageableDto<AnimeDto>>() {})
                animes.data.forEach(::println)
                assertEquals(1, animes.data.size)
                assertEquals(
                    "7th Time Loop: The Villainess Enjoys a Carefree Life Married to Her Worst Enemy!",
                    animes.data.first().name
                )
            }

            client.get("/api/v1/animes?country=${CountryCode.FR}&name=O&searchTypes=${LangType.VOICE}") {
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            }.apply {
                assertEquals(HttpStatusCode.OK, status)
                val animes = ObjectParser.fromJson(bodyAsText(), object : TypeToken<PageableDto<AnimeDto>>() {})
                animes.data.forEach(::println)
                assertEquals(1, animes.data.size)
                assertEquals("One Piece", animes.data.first().name)
            }
        }
    }

    @Test
    fun bugSearch() {
        testApplication {
            application {
                module()
            }

            client.get(
                "/api/v1/animes?country=${CountryCode.FR}&name=one&page=5&limit=6&searchTypes=${
                    LangType.entries.joinToString(
                        ","
                    )
                }"
            ) {
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            }.apply {
                assertEquals(HttpStatusCode.OK, status)
                val animes = ObjectParser.fromJson(bodyAsText(), object : TypeToken<PageableDto<AnimeDto>>() {})
                animes.data.forEach(::println)
                assertEquals(0, animes.data.size)
                assertEquals(2, animes.total)
            }
        }
    }

    @Test
    fun getFollowedAnimes() {
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

            client.get("/api/v1/animes?&page=1&limit=8") {
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            }.apply {
                assertEquals(HttpStatusCode.OK, status)
                val followedEpisodes = ObjectParser.fromJson(bodyAsText(), object : TypeToken<PageableDto<AnimeDto>>() {})
                assertEquals(2, followedEpisodes.data.size)
                assertEquals(2, followedEpisodes.total)
            }

            client.get("/api/v1/animes?page=1&limit=8") {
                header(HttpHeaders.Authorization, "Bearer $token")
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            }.apply {
                assertEquals(HttpStatusCode.OK, status)
                val followedEpisodes = ObjectParser.fromJson(bodyAsText(), object : TypeToken<PageableDto<AnimeDto>>() {})
                assertEquals(1, followedEpisodes.data.size)
                assertEquals(1, followedEpisodes.total)
            }
        }
    }
}