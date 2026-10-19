package fr.shikkanime.api.controllers

import fr.shikkanime.database.builders.impl.AnimeLocalizationMockKBuilder
import fr.shikkanime.database.builders.impl.AnimeMockKBuilder
import fr.shikkanime.database.builders.impl.AnimePlatformMockKBuilder
import fr.shikkanime.database.builders.impl.AnimeWithChildrenMockKBuilder
import fr.shikkanime.database.builders.impl.ImageMockKBuilder
import fr.shikkanime.database.services.AnimeService
import fr.shikkanime.ktor.ControllerBinder
import fr.shikkanime.ktor.configureDefaultModules
import fr.shikkanime.models.Country
import fr.shikkanime.models.ImageType
import fr.shikkanime.models.Platform
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.routing.*
import io.ktor.server.testing.*
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.extension.ExtendWith
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.uuid.Uuid

@ExtendWith(MockKExtension::class)
@DisplayName("tests for the anime controller")
class AnimeControllerTest {
    @MockK private lateinit var animeService: AnimeService

    @InjectMockKs private lateinit var animeController: AnimeController

    /**
     * Registers the controller under test on the test application.
     *
     * @receiver the Ktor test application.
     * @return the application routing the controller.
     * @see [ControllerBinder]
     */
    private fun Application.registerControllers() {
        routing {
            ControllerBinder.register(this, listOf(animeController))
        }
    }

    private val anime = AnimeWithChildrenMockKBuilder {
        anime = AnimeMockKBuilder {
            id = Uuid.random()
            normalizedTitle = "Title"
        }.build()
        images = listOf(
            ImageMockKBuilder {
                id = Uuid.random()
                type = ImageType.BANNER
                url = "https://example.test/banner.png"
            }.build()
        )
        localizations = listOf(
            AnimeLocalizationMockKBuilder {
                id = Uuid.random()
                locale = Country.FR.defaultLocale
                title = "Title"
                description = "Description"
            }.build()
        )
        platforms = listOf(
            AnimePlatformMockKBuilder {
                id = Uuid.random()
                platform = Platform.ANIMATION_DIGITAL_NETWORK
                platformId = "123"
            }.build()
        )
    }.build()

    @Nested
    @DisplayName("tests for listing the stored animes")
    inner class FindAllTests {
        @Test
        @DisplayName("should return 200 with the stored animes")
        fun `should return 200 with the stored animes`() =
            testApplication {
                // Given
                every { animeService.findAll() } returns listOf(anime)
                application {
                    configureDefaultModules()
                    registerControllers()
                }

                // When
                val response = client.get("/v1/animes")

                // Then
                assertEquals(HttpStatusCode.OK, response.status)
            }
    }

    @Nested
    @DisplayName("tests for reading one stored anime")
    inner class FindByIdTests {
        @Test
        @DisplayName("should return 200 with the stored anime")
        fun `should return 200 with the stored anime`() =
            testApplication {
                // Given
                every { animeService.findById(any()) } returns anime
                application {
                    configureDefaultModules()
                    registerControllers()
                }

                // When
                val response = client.get("/v1/animes/${anime.anime.id}")

                // Then
                assertEquals(HttpStatusCode.OK, response.status)
            }

        @Test
        @DisplayName("should return 404 when the anime is not stored")
        fun `should return 404 when the anime is not stored`() =
            testApplication {
                // Given
                every { animeService.findById(any()) } returns null
                application {
                    configureDefaultModules()
                    registerControllers()
                }

                // When
                val response = client.get("/v1/animes/${anime.anime.id}")

                // Then
                assertEquals(HttpStatusCode.NotFound, response.status)
            }
    }
}
