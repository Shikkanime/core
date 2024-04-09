package fr.shikkanime.controllers.api

import com.google.gson.reflect.TypeToken
import com.google.inject.Inject
import fr.shikkanime.converters.AbstractConverter
import fr.shikkanime.dtos.AnimeDto
import fr.shikkanime.dtos.MessageDto
import fr.shikkanime.dtos.PageableDto
import fr.shikkanime.dtos.WeeklyAnimesDto
import fr.shikkanime.entities.Anime
import fr.shikkanime.entities.Episode
import fr.shikkanime.entities.enums.CountryCode
import fr.shikkanime.entities.enums.EpisodeType
import fr.shikkanime.entities.enums.LangType
import fr.shikkanime.entities.enums.Platform
import fr.shikkanime.module
import fr.shikkanime.services.AnimeService
import fr.shikkanime.services.EpisodeService
import fr.shikkanime.services.ImageService
import fr.shikkanime.services.SimulcastService
import fr.shikkanime.utils.Constant
import fr.shikkanime.utils.ObjectParser
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.testing.*
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.File
import java.util.*

internal class AnimeControllerTest {
    class Token : TypeToken<PageableDto<AnimeDto>>()

    @Inject
    private lateinit var animeService: AnimeService

    @Inject
    private lateinit var simulcastService: SimulcastService

    @Inject
    private lateinit var episodeService: EpisodeService

    private var totalAnimes: Long = 0

    @BeforeEach
    fun setUp() {
        Constant.injector.injectMembers(this)

        val listFiles = ClassLoader.getSystemClassLoader().getResource("animes")?.file?.let { File(it).listFiles() }
        totalAnimes = listFiles?.size?.toLong() ?: 0

        listFiles
            ?.sortedBy { it.name.lowercase() }
            ?.forEach {
                animeService.save(
                    AbstractConverter.convert(
                        ObjectParser.fromJson(
                            it.readText(),
                            AnimeDto::class.java
                        ), Anime::class.java
                    )
                )
            }
    }

    @AfterEach
    fun tearDown() {
        animeService.deleteAll()
        simulcastService.deleteAll()
        animeService.preIndex()
        ImageService.cache.clear()
        ImageService.change.set(true)
        ImageService.saveCache()
    }

    @Test
    fun `conflit simulcast and name`() {
        testApplication {
            application {
                module()
            }

            client.get("/api/v1/animes?simulcast=${simulcastService.findAll().first().uuid}&name=One Piece").apply {
                assertEquals(HttpStatusCode.Conflict, status)
                val message = ObjectParser.fromJson(bodyAsText(), MessageDto::class.java)
                assertEquals(MessageDto.Type.ERROR, message.type)
                assertEquals("You can't use simulcast and name at the same time", message.message)
            }
        }
    }

    @Test
    fun `get all`() {
        testApplication {
            application {
                module()
            }

            client.get("/api/v1/animes").apply {
                assertEquals(HttpStatusCode.OK, status)
                val pageableDto = ObjectParser.fromJson(bodyAsText(), Token())
                assertEquals(totalAnimes, pageableDto.total)
                assertEquals("Dragon Ball Z", pageableDto.data[0].name)
                assertEquals("2024-01-01T00:00:00Z", pageableDto.data[0].releaseDateTime)
                assertEquals("WINTER", pageableDto.data[0].simulcasts!![0].season)
                assertEquals(2024, pageableDto.data[0].simulcasts!![0].year)
            }
        }
    }

    @Test
    fun `get sort name`() {
        testApplication {
            application {
                module()
            }

            client.get("/api/v1/animes?name=One Piece&sort=releaseDateTime").apply {
                assertEquals(HttpStatusCode.Conflict, status)
                val message = ObjectParser.fromJson(bodyAsText(), MessageDto::class.java)
                assertEquals(MessageDto.Type.ERROR, message.type)
                assertEquals("You can't use sort and desc with name", message.message)
            }
        }
    }

    @Test
    fun `get name`() {
        testApplication {
            application {
                module()
            }

            client.get("/api/v1/animes?name=naruto").apply {
                assertEquals(HttpStatusCode.OK, status)
                val json = bodyAsText()
                println(json)
                val pageable = ObjectParser.fromJson(json, Token())
                assertEquals(1, pageable.data.size)
                assertEquals(1, pageable.total)
                assertEquals("Naruto", pageable.data[0].name)
                assertEquals("2024-01-01T00:00:00Z", pageable.data[0].releaseDateTime)
                assertEquals("WINTER", pageable.data[0].simulcasts!![0].season)
                assertEquals(2024, pageable.data[0].simulcasts!![0].year)
            }
        }
    }

    @Test
    fun `get name #2`() {
        testApplication {
            application {
                module()
            }

            client.get("/api/v1/animes?name=piece").apply {
                assertEquals(HttpStatusCode.OK, status)
                val json = bodyAsText()
                println(json)
                val pageable = ObjectParser.fromJson(json, Token())
                assertEquals(2, pageable.data.size)
                assertEquals(2, pageable.total)
                assertEquals("One Piece", pageable.data[0].name)
                assertEquals("2024-01-01T00:00:00Z", pageable.data[0].releaseDateTime)
                assertEquals("WINTER", pageable.data[0].simulcasts!![0].season)
                assertEquals(2024, pageable.data[0].simulcasts!![0].year)
            }
        }
    }

    @Test
    fun `get by simulcast sort name desc`() {
        testApplication {
            application {
                module()
            }

            client.get("/api/v1/animes?simulcast=${simulcastService.findAll().first().uuid}&sort=name&desc=name")
                .apply {
                    assertEquals(HttpStatusCode.OK, status)
                    val pageable = ObjectParser.fromJson(bodyAsText(), Token())
                    assertEquals(totalAnimes, pageable.total)
                    assertEquals("Two Piece", pageable.data[0].name)
                    assertEquals("2024-01-01T00:00:00Z", pageable.data[0].releaseDateTime)
                    assertEquals("WINTER", pageable.data[0].simulcasts!![0].season)
                    assertEquals(2024, pageable.data[0].simulcasts!![0].year)
                }
        }
    }

    @Test
    fun `get by simulcast and country`() {
        testApplication {
            application {
                module()
            }

            client.get("/api/v1/animes?country=${CountryCode.FR}&simulcast=${simulcastService.findAll().first().uuid}")
                .apply {
                    assertEquals(HttpStatusCode.OK, status)
                    val pageable = ObjectParser.fromJson(bodyAsText(), Token())
                    assertEquals(totalAnimes, pageable.total)
                    assertEquals("Dragon Ball Z", pageable.data[0].name)
                    assertEquals("2024-01-01T00:00:00Z", pageable.data[0].releaseDateTime)
                    assertEquals("WINTER", pageable.data[0].simulcasts!![0].season)
                    assertEquals(2024, pageable.data[0].simulcasts!![0].year)
                }
        }
    }

    @Test
    fun `get weekly`() {
        (1..10).forEach { number ->
            animeService.findAll().forEach { anime ->
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
                        description = "Description $number",
                        url = "https://www.google.com",
                        image = "https://pbs.twimg.com/profile_banners/1726908281640091649/1700562801/1500x500",
                        duration = 1420
                    )
                )
            }
        }

        testApplication {
            application {
                module()
            }

            client.get("/api/v1/animes/weekly?date=2024-01-01")
                .apply {
                    assertEquals(HttpStatusCode.OK, status)
                    val weeklyAnimesDtos = ObjectParser.fromJson(bodyAsText(), Array<WeeklyAnimesDto>::class.java)
                    assertEquals(7, weeklyAnimesDtos.size)
                    assertEquals(8, weeklyAnimesDtos[0].releases.size)
                }
        }

        episodeService.deleteAll()
    }

    @Test
    fun `get weekly invalid date`() {
        testApplication {
            application {
                module()
            }

            client.get("/api/v1/animes/weekly?date=abc")
                .apply {
                    assertEquals(HttpStatusCode.BadRequest, status)
                }
        }
    }

    @Test
    fun `get weekly with no parameter`() {
        testApplication {
            application {
                module()
            }

            client.get("/api/v1/animes/weekly")
                .apply {
                    assertEquals(HttpStatusCode.OK, status)
                    val weeklyAnimesDtos = ObjectParser.fromJson(bodyAsText(), Array<WeeklyAnimesDto>::class.java)
                    assertEquals(7, weeklyAnimesDtos.size)
                    assertEquals(0, weeklyAnimesDtos.sumOf { it.releases.size })
                }
        }
    }
}