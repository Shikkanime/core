package fr.shikkanime.controllers.api

import com.google.gson.reflect.TypeToken
import com.google.inject.Inject
import fr.shikkanime.converters.AbstractConverter
import fr.shikkanime.dtos.AnimeDto
import fr.shikkanime.dtos.EpisodeDto
import fr.shikkanime.dtos.PageableDto
import fr.shikkanime.entities.Anime
import fr.shikkanime.entities.Episode
import fr.shikkanime.entities.enums.EpisodeType
import fr.shikkanime.entities.enums.LangType
import fr.shikkanime.entities.enums.Platform
import fr.shikkanime.module
import fr.shikkanime.services.AnimeService
import fr.shikkanime.services.EpisodeService
import fr.shikkanime.utils.Constant
import fr.shikkanime.utils.ObjectParser
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.testing.*
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.File
import java.util.*

class EpisodeControllerTest {
    class Token : TypeToken<PageableDto<EpisodeDto>>()

    @Inject
    private lateinit var animeService: AnimeService

    @Inject
    private lateinit var episodeService: EpisodeService

    @BeforeEach
    fun setUp() {
        Constant.injector.injectMembers(this)

        val listFiles = File(ClassLoader.getSystemClassLoader().getResource("animes")?.file).listFiles()

        listFiles
            ?.sortedBy { it.name.lowercase() }
            ?.forEach {
                val anime = animeService.save(
                    AbstractConverter.convert(
                        ObjectParser.fromJson(
                            it.readText(),
                            AnimeDto::class.java
                        ), Anime::class.java
                    )
                )

                (1..10).forEach { number ->
                    episodeService.save(
                        Episode(
                            platform = Platform.entries.random(),
                            anime = anime,
                            episodeType = EpisodeType.entries.random(),
                            langType = LangType.entries.random(),
                            hash = UUID.randomUUID().toString(),
                            releaseDateTime = anime.releaseDateTime,
                            season = 1,
                            number = number,
                            title = "Episode $number",
                            url = "https://www.google.com",
                            image = "https://pbs.twimg.com/profile_banners/1726908281640091649/1700562801/1500x500",
                            duration = 1420
                        )
                    )
                }
            }
    }

    @AfterEach
    fun tearDown() {
        episodeService.deleteAll()
        animeService.deleteAll()
    }

    @Test
    fun `get all`() {
        testApplication {
            application {
                module()
            }

            client.get("/api/v1/episodes?limit=12").apply {
                assertEquals(HttpStatusCode.OK, status)
                val pageableDto = ObjectParser.fromJson(bodyAsText(), Token())
                assertEquals(12, pageableDto.data.size)
                assertEquals(40, pageableDto.total)
                assertEquals("Dragon Ball Z", pageableDto.data[0].anime.name)
                assertEquals("2024-01-01T00:00:00Z", pageableDto.data[0].releaseDateTime)
                assertEquals(1, pageableDto.data[0].season)
                assertEquals(1, pageableDto.data[0].season)
            }
        }
    }

    @Test
    fun `get all sorted`() {
        testApplication {
            application {
                module()
            }

            client.get("/api/v1/episodes?sort=releaseDateTime,season,number,episodeType,langType&desc=releaseDateTime,season,number,langType&limit=12")
                .apply {
                    assertEquals(HttpStatusCode.OK, status)
                    val pageableDto = ObjectParser.fromJson(bodyAsText(), Token())
                    assertEquals(12, pageableDto.data.size)
                    assertEquals(40, pageableDto.total)
                    assertNotNull(pageableDto.data[0].anime.name)
                    assertEquals("2024-01-01T00:00:00Z", pageableDto.data[0].releaseDateTime)
                    assertEquals(1, pageableDto.data[0].season)
                    assertEquals(10, pageableDto.data[0].number)
                }
        }
    }

    @Test
    fun `get all sorted by anime`() {
        testApplication {
            application {
                module()
            }

            val anime = animeService.findAll().first()

            client.get("/api/v1/episodes?anime=${anime.uuid}&sort=releaseDateTime,season,number,episodeType,langType&desc=releaseDateTime,season,number,langType&limit=12")
                .apply {
                    assertEquals(HttpStatusCode.OK, status)
                    val pageableDto = ObjectParser.fromJson(bodyAsText(), Token())
                    assertEquals(10, pageableDto.data.size)
                    assertEquals(10, pageableDto.total)
                    assertEquals(anime.uuid.toString(), pageableDto.data[0].anime.uuid.toString())
                    assertEquals("2024-01-01T00:00:00Z", pageableDto.data[0].releaseDateTime)
                    assertEquals(1, pageableDto.data[0].season)
                    assertEquals(10, pageableDto.data[0].number)
                }
        }
    }
}