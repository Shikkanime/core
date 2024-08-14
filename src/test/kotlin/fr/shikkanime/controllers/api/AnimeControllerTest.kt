package fr.shikkanime.controllers.api

import com.google.gson.reflect.TypeToken
import fr.shikkanime.dtos.*
import fr.shikkanime.module
import fr.shikkanime.services.*
import fr.shikkanime.utils.ObjectParser
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.testing.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

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
                val followedAnimesUUID = memberFollowAnimeService.findAllFollowedAnimesUUID(findPrivateMember!!)
                assertNotNull(findPrivateMember)
                assertEquals(1, followedAnimesUUID.size)
                assertEquals(anime.uuid, followedAnimesUUID.first())
            }

            client.get("/api/v1/animes/weekly?date=2024-01-01") {
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            }.apply {
                assertEquals(HttpStatusCode.OK, status)
                val weeklyAnimesDto = ObjectParser.fromJson(bodyAsText(), Array<WeeklyAnimesDto>::class.java).flatMap { it.releases }
                assertEquals(3, weeklyAnimesDto.size)
                assertTrue(weeklyAnimesDto.all { it.anime.seasons.isNotEmpty() })
            }

            client.get("/api/v1/animes/weekly?date=2024-01-01") {
                header(HttpHeaders.Authorization, "Bearer $token")
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            }.apply {
                assertEquals(HttpStatusCode.OK, status)
                val weeklyAnimesDto = ObjectParser.fromJson(bodyAsText(), Array<WeeklyAnimesDto>::class.java).flatMap { it.releases }
                assertEquals(2, weeklyAnimesDto.size)
                assertEquals(anime.uuid, weeklyAnimesDto.first().anime.uuid)
                assertTrue(weeklyAnimesDto.all { it.anime.seasons.isNotEmpty() })
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
                val followedAnimesUUID = memberFollowAnimeService.findAllFollowedAnimesUUID(findPrivateMember!!)
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
                val followedEpisodesUUID = memberFollowEpisodeService.findAllFollowedEpisodesUUID(findPrivateMember!!)
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
}