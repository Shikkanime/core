package fr.shikkanime.controllers.api

import com.google.inject.Inject
import fr.shikkanime.converters.AbstractConverter
import fr.shikkanime.dtos.animes.AnimeDto
import fr.shikkanime.entities.Anime
import fr.shikkanime.module
import fr.shikkanime.services.AnimeService
import fr.shikkanime.services.ImageService
import fr.shikkanime.services.SimulcastService
import fr.shikkanime.utils.Constant
import fr.shikkanime.utils.ObjectParser
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.server.testing.*
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.File

class AttachmentControllerTest {
    @Inject
    private lateinit var animeService: AnimeService

    @Inject
    private lateinit var simulcastService: SimulcastService

    @BeforeEach
    fun setUp() {
        Constant.injector.injectMembers(this)

        val listFiles = ClassLoader.getSystemClassLoader().getResource("animes")?.file?.let { File(it).listFiles() }

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
    fun `get all sorted`() {
        testApplication {
            application {
                module()
            }

            client.get("/api/v1/attachments?uuid=${animeService.findAll().first().uuid}&type=IMAGE")
                .apply {
                    assertEquals(HttpStatusCode.OK, status)
                    assertNotNull(body())
                }
        }
    }
}