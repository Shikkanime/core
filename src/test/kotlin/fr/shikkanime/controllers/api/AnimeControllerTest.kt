package fr.shikkanime.controllers.api

import com.google.gson.reflect.TypeToken
import com.google.inject.Inject
import fr.shikkanime.converters.AbstractConverter
import fr.shikkanime.dtos.AnimeDto
import fr.shikkanime.dtos.MessageDto
import fr.shikkanime.dtos.PageableDto
import fr.shikkanime.entities.Anime
import fr.shikkanime.module
import fr.shikkanime.services.AnimeService
import fr.shikkanime.services.SimulcastService
import fr.shikkanime.utils.Constant
import fr.shikkanime.utils.Database
import fr.shikkanime.utils.ObjectParser
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.testing.*
import org.hibernate.search.mapper.orm.Search
import java.io.File
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class AnimeControllerTest {
    class Token : TypeToken<PageableDto<AnimeDto>>()

    @Inject
    private lateinit var database: Database

    @Inject
    private lateinit var animeService: AnimeService

    @Inject
    private lateinit var simulcastService: SimulcastService

    private var totalAnimes: Long = 0

    @BeforeTest
    fun setUp() {
        Constant.injector.injectMembers(this)

        val listFiles = File(ClassLoader.getSystemClassLoader().getResource("animes")?.file).listFiles()
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

    @AfterTest
    fun tearDown() {
        animeService.deleteAll()
        simulcastService.deleteAll()
        Search.session(database.getEntityManager()).massIndexer(Anime::class.java).startAndWait()
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
}