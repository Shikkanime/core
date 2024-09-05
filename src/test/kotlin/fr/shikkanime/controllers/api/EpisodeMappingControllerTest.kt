package fr.shikkanime.controllers.api

import com.google.gson.reflect.TypeToken
import fr.shikkanime.dtos.AllFollowedEpisodeDto
import fr.shikkanime.dtos.GenericDto
import fr.shikkanime.dtos.PageableDto
import fr.shikkanime.dtos.mappings.EpisodeMappingDto
import fr.shikkanime.module
import fr.shikkanime.utils.ObjectParser
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.testing.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class EpisodeMappingControllerTest : AbstractControllerTest() {
    @Test
    fun getAll() {
        testApplication {
            application {
                module()
            }

            client.get("/api/v1/episode-mappings?sort=lastReleaseDateTime,animeName,season,episodeType,number&desc=lastReleaseDateTime,animeName,season,episodeType,number&page=1&limit=4") {
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            }.apply {
                assertEquals(HttpStatusCode.OK, status)
                val episodeMappingsDto = ObjectParser.fromJson(bodyAsText(), PageableDto::class.java)
                assertEquals(4, episodeMappingsDto.data.size)
                assertTrue(episodeMappingsDto.total > 4)
            }
        }
    }

    @Test
    fun getFollowedEpisodes() {
        testApplication {
            application {
                module()
            }

            val (identifier, token) = registerAndLogin()
            val anime = animeService.findAll().first()
            val episodes = episodeMappingService.findAllByAnime(anime)

            client.put("/api/v1/members/follow-all-episodes") {
                header(HttpHeaders.Authorization, "Bearer $token")
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(ObjectParser.toJson(GenericDto(anime.uuid!!)))
            }.apply {
                assertEquals(HttpStatusCode.OK, status)
                val allFollowedEpisodeDto = ObjectParser.fromJson(bodyAsText(), AllFollowedEpisodeDto::class.java)
                val findPrivateMember = memberService.findByIdentifier(identifier)
                assertNotNull(findPrivateMember)
                assertEquals(episodes.size, allFollowedEpisodeDto.data.size)
                assertEquals(episodes.sumOf { it.duration }, allFollowedEpisodeDto.duration)
            }

            client.get("/api/v1/episode-mappings?page=1&limit=8") {
                header(HttpHeaders.Authorization, "Bearer $token")
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            }.apply {
                assertEquals(HttpStatusCode.OK, status)
                val followedEpisodes =
                    ObjectParser.fromJson(bodyAsText(), object : TypeToken<PageableDto<EpisodeMappingDto>>() {})
                assertEquals(8, followedEpisodes.data.size)
                assertEquals(episodes.size.toLong(), followedEpisodes.total)
            }
        }
    }
}