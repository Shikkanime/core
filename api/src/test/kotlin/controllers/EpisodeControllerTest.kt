package fr.shikkanime.api.controllers

import fr.shikkanime.api.dtos.toDto
import fr.shikkanime.api.usecases.FindCatalogUseCase
import fr.shikkanime.core.TimeUtils
import fr.shikkanime.core.pagination.PageRequest
import fr.shikkanime.database.builders.impl.EpisodeLocalizationMockKBuilder
import fr.shikkanime.database.builders.impl.EpisodeMockKBuilder
import fr.shikkanime.database.builders.impl.EpisodeReleaseMockKBuilder
import fr.shikkanime.database.builders.impl.EpisodeWithChildrenMockKBuilder
import fr.shikkanime.database.builders.impl.ImageMockKBuilder
import fr.shikkanime.ktor.ControllerBinder
import fr.shikkanime.ktor.configureDefaultModules
import fr.shikkanime.ktor.pagination.PageDto
import fr.shikkanime.models.EpisodeType
import fr.shikkanime.models.ImageType
import fr.shikkanime.models.Locale
import fr.shikkanime.models.Platform
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.routing.*
import io.ktor.server.testing.*
import io.mockk.coEvery
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.slot
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.extension.ExtendWith
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.uuid.Uuid

@ExtendWith(MockKExtension::class)
@DisplayName("tests for the episode controller")
class EpisodeControllerTest {
    @MockK private lateinit var findCatalogUseCase: FindCatalogUseCase

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
        releases = listOf(
            EpisodeReleaseMockKBuilder {
                id = Uuid.random()
                platform = Platform.ANIMATION_DIGITAL_NETWORK
                platformId = "3001"
                url = "https://example.test/episodes/3001"
                audioLocale = Locale.FR_FR
                uncensored = false
                releaseDateTime = TimeUtils.now()
            }.build()
        )
    }.build()

    private val episodeDto = episode.toDto()

    private val episodePage = PageDto(
        data = listOf(episodeDto),
        page = 1,
        size = 20,
        totalPages = 1,
        totalElements = 1
    )

    @Nested
    @DisplayName("tests for listing the episodes of an anime")
    inner class FindAllByAnimeIdTests {
        @Test
        @DisplayName("should return 200 with one page of the episodes of the anime")
        fun `should return 200 with one page of the episodes of the anime`() =
            testApplication {
                // Given
                coEvery { findCatalogUseCase.findEpisodePage(any(), any()) } returns episodePage
                application {
                    configureDefaultModules()
                    registerControllers()
                }

                // When
                val response = client.get("/v1/episodes?animeId=${episode.episode.animeId}&page=1&size=20")

                // Then
                assertEquals(HttpStatusCode.OK, response.status)
            }

        @Test
        @DisplayName("should forward the anime identifier and the requested page bounds")
        fun `should forward the anime identifier and the requested page bounds`() =
            testApplication {
                // Given
                val animeId = slot<Uuid>()
                val request = slot<PageRequest>()
                coEvery {
                    findCatalogUseCase.findEpisodePage(capture(animeId), capture(request))
                } returns episodePage
                application {
                    configureDefaultModules()
                    registerControllers()
                }

                // When
                client.get("/v1/episodes?animeId=${episode.episode.animeId}&page=3&size=10")

                // Then
                assertEquals(episode.episode.animeId, animeId.captured)
                assertEquals(PageRequest(3, 10), request.captured)
            }

        @Test
        @DisplayName("should return 400 when the requested page is below the minimum")
        fun `should return 400 when the requested page is below the minimum`() =
            testApplication {
                // Given
                application {
                    configureDefaultModules()
                    registerControllers()
                }

                // When
                val response = client.get("/v1/episodes?animeId=${episode.episode.animeId}&page=0")

                // Then
                assertEquals(HttpStatusCode.BadRequest, response.status)
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
                coEvery { findCatalogUseCase.findEpisode(any()) } returns episodeDto
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
                coEvery { findCatalogUseCase.findEpisode(any()) } returns null
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
