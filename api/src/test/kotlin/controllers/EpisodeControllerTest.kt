package fr.shikkanime.api.controllers

import fr.shikkanime.core.TimeUtils
import fr.shikkanime.database.builders.impl.EpisodeLocalizationMockKBuilder
import fr.shikkanime.database.builders.impl.EpisodeMockKBuilder
import fr.shikkanime.database.builders.impl.EpisodePlatformMockKBuilder
import fr.shikkanime.database.builders.impl.EpisodeWithChildrenMockKBuilder
import fr.shikkanime.database.builders.impl.ImageMockKBuilder
import fr.shikkanime.database.services.EpisodeService
import fr.shikkanime.ktor.ControllerBinder
import fr.shikkanime.ktor.configureDefaultModules
import fr.shikkanime.models.EpisodeType
import fr.shikkanime.models.ImageType
import fr.shikkanime.models.Locale
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
@DisplayName("tests for the episode controller")
class EpisodeControllerTest {
    @MockK private lateinit var episodeService: EpisodeService

    @InjectMockKs private lateinit var episodeController: EpisodeController

    /**
     * Registers the controller under test on the test application.
     *
     * @receiver the Ktor test application.
     * @return the application routing the controller.
     * @see [ControllerBinder]
     */
    private fun Application.registerControllers() {
        routing {
            ControllerBinder.register(this, listOf(episodeController))
        }
    }

    private val episode = EpisodeWithChildrenMockKBuilder {
        episode = EpisodeMockKBuilder {
            id = Uuid.random()
            animeId = Uuid.random()
            season = 1
            number = 1
            episodeType = EpisodeType.EPISODE
            releaseDateTime = TimeUtils.now()
            duration = 1440
        }.build()
        images = listOf(
            ImageMockKBuilder {
                id = Uuid.random()
                type = ImageType.BANNER
                url = "https://example.test/episode.png"
            }.build()
        )
        localizations = listOf(
            EpisodeLocalizationMockKBuilder {
                id = Uuid.random()
                locale = Locale.FR_FR
                title = "Episode"
                description = "Description"
            }.build()
        )
        platforms = listOf(
            EpisodePlatformMockKBuilder {
                id = Uuid.random()
                platform = Platform.ANIMATION_DIGITAL_NETWORK
                platformId = "3001"
                url = "https://example.test/episodes/3001"
                audioLocales = setOf(Locale.FR_FR)
                uncensored = false
                releaseDateTime = TimeUtils.now()
            }.build()
        )
    }.build()

    @Nested
    @DisplayName("tests for listing the episodes of an anime")
    inner class FindAllByAnimeIdTests {
        @Test
        @DisplayName("should return 200 with the episodes of the anime")
        fun `should return 200 with the episodes of the anime`() =
            testApplication {
                // Given
                every { episodeService.findAllByAnimeId(any()) } returns listOf(episode)
                application {
                    configureDefaultModules()
                    registerControllers()
                }

                // When
                val response = client.get("/v1/episodes?animeId=${episode.episode.animeId}")

                // Then
                assertEquals(HttpStatusCode.OK, response.status)
            }
    }

    @Nested
    @DisplayName("tests for reading one stored episode")
    inner class FindByIdTests {
        @Test
        @DisplayName("should return 200 with the stored episode")
        fun `should return 200 with the stored episode`() =
            testApplication {
                // Given
                every { episodeService.findById(any()) } returns episode
                application {
                    configureDefaultModules()
                    registerControllers()
                }

                // When
                val response = client.get("/v1/episodes/${episode.episode.id}")

                // Then
                assertEquals(HttpStatusCode.OK, response.status)
            }

        @Test
        @DisplayName("should return 404 when the episode is not stored")
        fun `should return 404 when the episode is not stored`() =
            testApplication {
                // Given
                every { episodeService.findById(any()) } returns null
                application {
                    configureDefaultModules()
                    registerControllers()
                }

                // When
                val response = client.get("/v1/episodes/${episode.episode.id}")

                // Then
                assertEquals(HttpStatusCode.NotFound, response.status)
            }
    }
}
