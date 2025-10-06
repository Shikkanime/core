package fr.shikkanime.controllers.api.v2

import fr.shikkanime.controllers.api.AbstractControllerTest
import fr.shikkanime.dtos.PageableDto
import fr.shikkanime.entities.Anime
import fr.shikkanime.entities.EpisodeMapping
import fr.shikkanime.entities.EpisodeVariant
import fr.shikkanime.entities.enums.CountryCode
import fr.shikkanime.entities.enums.EpisodeType
import fr.shikkanime.entities.enums.Platform
import fr.shikkanime.module
import fr.shikkanime.utils.ObjectParser
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.testing.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.ZonedDateTime

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

    @Test
    fun `release on 30 minutes difference`() {
        attachmentService.clearPool()
        database.truncate()
        database.clearCache()

        val firstReleaseDateTime = ZonedDateTime.parse("2025-07-06T09:30:00Z")

        val anime = animeService.save(
            Anime(
                countryCode = countryCode,
                name = "Puniru is a Kawaii Slime",
                slug = "puniru-is-a-kawaii-slime",
                releaseDateTime = firstReleaseDateTime
            )
        )

        val mapping = episodeMappingService.save(
            EpisodeMapping(
                anime = anime,
                releaseDateTime = firstReleaseDateTime,
                episodeType = EpisodeType.EPISODE,
                season = 2,
                number = 1,
                duration = 1440,
                title = "Episode 1",
                description = "Description 1",
            )
        )

        episodeVariantService.save(
            EpisodeVariant(
                mapping = mapping,
                releaseDateTime = firstReleaseDateTime,
                platform = Platform.ANIM,
                audioLocale = "ja-JP",
                identifier = "FR-ANIM-29380-JA-JP",
                url = "https://animationdigitalnetwork.com/video/1259-puniru-is-a-kawaii-slime/29380-episode-1",
            )
        )

        episodeVariantService.save(
            EpisodeVariant(
                mapping = mapping,
                releaseDateTime = firstReleaseDateTime.plusMinutes(30),
                platform = Platform.CRUN,
                audioLocale = "ja-JP",
                identifier = "FR-CRUN-GJWUQ18WG-JA-JP",
                url = "https://www.crunchyroll.com/fr/watch/GJWUQ18WG/puniru-returns",
            )
        )

        episodeVariantService.preIndex()

        testApplication {
            application {
                module()
            }

            client.get("/api/v2/episode-mappings?country=${countryCode}&page=1&limit=4") {
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            }.apply {
                assertEquals(HttpStatusCode.OK, status)
                val episodeMappingsDto = ObjectParser.fromJson(bodyAsText(), PageableDto::class.java)
                assertEquals(1, episodeMappingsDto.total)
                assertEquals(1, episodeMappingsDto.data.size)
            }
        }
    }
}