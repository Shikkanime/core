package fr.shikkanime.controllers.api

import com.google.inject.Inject
import fr.shikkanime.converters.AbstractConverter
import fr.shikkanime.dtos.AnimeDto
import fr.shikkanime.dtos.MessageDto
import fr.shikkanime.entities.Anime
import fr.shikkanime.module
import fr.shikkanime.services.AnimeService
import fr.shikkanime.services.SimulcastService
import fr.shikkanime.utils.Constant
import fr.shikkanime.utils.ObjectParser
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.testing.*
import java.io.File
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class AnimeControllerTest {
    @Inject
    private lateinit var animeService: AnimeService

    @Inject
    private lateinit var simulcastService: SimulcastService

    @BeforeTest
    fun setUp() {
        Constant.injector.injectMembers(this)

        File(ClassLoader.getSystemClassLoader().getResource("animes")?.file).listFiles()
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

    @AfterTest
    fun tearDown() {
        animeService.deleteAll()
        simulcastService.deleteAll()
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
                val animes = ObjectParser.fromJson(bodyAsText(), Array<AnimeDto>::class.java)
                assertEquals(2, animes.size)
                assertEquals("Naruto", animes[0].name)
                assertEquals("2024-01-01T00:00:00Z", animes[0].releaseDateTime)
                assertEquals("WINTER", animes[0].simulcasts!![0].season)
                assertEquals(2024, animes[0].simulcasts!![0].year)
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
                val animes = ObjectParser.fromJson(bodyAsText(), Array<AnimeDto>::class.java)
                assertEquals(1, animes.size)
                assertEquals("Naruto", animes[0].name)
                assertEquals("2024-01-01T00:00:00Z", animes[0].releaseDateTime)
                assertEquals("WINTER", animes[0].simulcasts!![0].season)
                assertEquals(2024, animes[0].simulcasts!![0].year)
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
                    val animes = ObjectParser.fromJson(bodyAsText(), Array<AnimeDto>::class.java)
                    assertEquals(2, animes.size)
                    assertEquals("One Piece", animes[0].name)
                    assertEquals("2024-01-01T00:00:00Z", animes[0].releaseDateTime)
                    assertEquals("WINTER", animes[0].simulcasts!![0].season)
                    assertEquals(2024, animes[0].simulcasts!![0].year)
                }
        }
    }
}