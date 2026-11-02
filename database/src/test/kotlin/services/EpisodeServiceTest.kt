package fr.shikkanime.database.services

import fr.shikkanime.core.TimeUtils
import fr.shikkanime.database.builders.impl.EpisodeLocalizationMockKBuilder
import fr.shikkanime.database.builders.impl.EpisodeMockKBuilder
import fr.shikkanime.database.builders.impl.EpisodeReleaseMockKBuilder
import fr.shikkanime.database.builders.impl.EpisodeWithChildrenMockKBuilder
import fr.shikkanime.database.builders.impl.ImageMockKBuilder
import fr.shikkanime.database.repositories.EpisodeLocalizationRepository
import fr.shikkanime.database.repositories.EpisodeReleaseRepository
import fr.shikkanime.database.repositories.EpisodeRepository
import fr.shikkanime.database.repositories.ImageRepository
import fr.shikkanime.database.services.impl.EpisodeServiceImpl
import fr.shikkanime.models.EpisodeLocalization
import fr.shikkanime.models.EpisodeRelease
import fr.shikkanime.models.EpisodeType
import fr.shikkanime.models.EpisodeWithChildren
import fr.shikkanime.models.Image
import fr.shikkanime.models.ImageType
import fr.shikkanime.models.Locale
import fr.shikkanime.models.Platform
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.extension.ExtendWith
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.uuid.Uuid

@ExtendWith(MockKExtension::class)
@DisplayName("tests for the episode service")
class EpisodeServiceTest {
    @MockK private lateinit var episodeRepository: EpisodeRepository
    @MockK private lateinit var localizationRepository: EpisodeLocalizationRepository
    @MockK private lateinit var releaseRepository: EpisodeReleaseRepository
    @MockK private lateinit var imageRepository: ImageRepository

    @InjectMockKs private lateinit var episodeService: EpisodeServiceImpl

    /**
     * Builds the aggregate fixture around the canonical root episode of these tests.
     *
     * @param configuration lambda receiving the builder to set the children of the aggregate.
     * @return the aggregate fixture around the "Episode" root episode.
     * @see [EpisodeWithChildrenMockKBuilder]
     */
    private fun episodeWithChildrenFixture(
        configuration: EpisodeWithChildrenMockKBuilder.() -> Unit
    ): EpisodeWithChildren =
        EpisodeWithChildrenMockKBuilder {
            episode = EpisodeMockKBuilder {
                id = Uuid.random()
                animeId = Uuid.random()
                season = 1
                number = 1
                episodeType = EpisodeType.EPISODE
                releaseDateTime = TimeUtils.now()
                duration = 1440
                createdAt = TimeUtils.now()
                updatedAt = TimeUtils.now()
            }.build()
            configuration()
        }.build()


    @Nested
    @DisplayName("tests for the 'save' method")
    inner class SaveTests {
        @Test
        @DisplayName("should persist the identity and every child of the aggregate")
        fun `should persist the identity and every child of the aggregate`() {
            // Given
            val episode = episodeWithChildrenFixture {
                localizations = listOf(
                    EpisodeLocalizationMockKBuilder {
                        id = Uuid.random()
                        createdAt = TimeUtils.now()
                        updatedAt = TimeUtils.now()
                        locale = Locale.FR_FR
                        title = "Episode"
                        description = "Description"
                    }.build()
                )
                releases = listOf(
                    EpisodeReleaseMockKBuilder {
                        id = Uuid.random()
                        createdAt = TimeUtils.now()
                        updatedAt = TimeUtils.now()
                        platform = Platform.ANIMATION_DIGITAL_NETWORK
                        platformId = "3001"
                        url = "https://example.test/episodes/3001"
                        audioLocale = Locale.FR_FR
                        uncensored = false
                        releaseDateTime = TimeUtils.now()
                    }.build()
                )
                images = listOf(
                    ImageMockKBuilder {
                        id = Uuid.random()
                        createdAt = TimeUtils.now()
                        updatedAt = TimeUtils.now()
                        entityId = Uuid.random()
                        type = ImageType.BANNER
                        url = "https://example.test/episode.png"
                    }.build()
                )
            }
            val root = episode.episode

            every { episodeRepository.save(episode) } returns root
            every { localizationRepository.save(any()) } returns episode.localizations.single()
            every { releaseRepository.save(any()) } returns episode.releases.single()
            every { imageRepository.save(any()) } returns episode.images.single()

            // When
            val saved = episodeService.save(episode)

            // Then
            verify(exactly = 1) { episodeRepository.save(episode) }
            verify(exactly = 1) { localizationRepository.save(any()) }
            verify(exactly = 1) { releaseRepository.save(any()) }
            verify(exactly = 1) { imageRepository.save(any()) }
            assertEquals(episode.episode.id, saved.episode.id)
        }

        @Test
        @DisplayName("should store images under the episode identifier whatever owner the image carries")
        fun `should store images under the episode identifier whatever owner the image carries`() {
            // Given
            val episode = episodeWithChildrenFixture {
                localizations = emptyList()
                releases = emptyList()
                images = listOf(
                    ImageMockKBuilder {
                        id = Uuid.random()
                        createdAt = TimeUtils.now()
                        updatedAt = TimeUtils.now()
                        entityId = Uuid.random()
                        type = ImageType.BANNER
                        url = "https://example.test/episode.png"
                    }.build()
                )
            }
            val stored = slot<Image>()

            every { episodeRepository.save(episode) } returns episode.episode
            every { imageRepository.save(capture(stored)) } returns episode.images.single()

            // When
            episodeService.save(episode)

            // Then
            assertEquals(episode.episode.id, stored.captured.entityId)
        }

        @Test
        @DisplayName("should store children under the episode identifier whatever owner the child carries")
        fun `should store children under the episode identifier whatever owner the child carries`() {
            // Given
            val episode = episodeWithChildrenFixture {
                localizations = listOf(
                    EpisodeLocalizationMockKBuilder {
                        id = Uuid.random()
                        episodeId = Uuid.random()
                        locale = Locale.FR_FR
                        title = "Episode"
                        description = "Description"
                    }.build()
                )
                releases = listOf(
                    EpisodeReleaseMockKBuilder {
                        id = Uuid.random()
                        episodeId = Uuid.random()
                        platform = Platform.ANIMATION_DIGITAL_NETWORK
                        platformId = "3001"
                        url = "https://example.test/episodes/3001"
                        audioLocale = Locale.FR_FR
                        uncensored = false
                        releaseDateTime = TimeUtils.now()
                    }.build()
                )
                images = emptyList()
            }
            val storedLocalization = slot<EpisodeLocalization>()
            val storedPlatform = slot<EpisodeRelease>()

            every { episodeRepository.save(episode) } returns episode.episode
            every { localizationRepository.save(capture(storedLocalization)) } returns episode.localizations.single()
            every { releaseRepository.save(capture(storedPlatform)) } returns episode.releases.single()

            // When
            episodeService.save(episode)

            // Then
            assertEquals(episode.episode.id, storedLocalization.captured.episodeId)
            assertEquals(episode.episode.id, storedPlatform.captured.episodeId)
        }

    }

    @Nested
    @DisplayName("tests for the read methods")
    inner class ReadTests {
        @Test
        @DisplayName("should return null when the episode does not exist")
        fun `should return null when the episode does not exist`() {
            // Given
            val unknown = Uuid.random()
            every { episodeRepository.findModelById(unknown) } returns null

            // When
            val found = episodeService.findById(unknown)

            // Then
            assertNull(found)
        }

        @Test
        @DisplayName("should return the aggregate read from the episode repository")
        fun `should return the aggregate read from the episode repository`() {
            // Given
            val episode = episodeWithChildrenFixture {
                localizations = listOf(
                    EpisodeLocalizationMockKBuilder {
                        id = Uuid.random()
                        locale = Locale.FR_FR
                        title = "Episode"
                    }.build()
                )
                releases = emptyList()
                images = emptyList()
            }
            every { episodeRepository.findModelById(any()) } returns episode
            every { episodeRepository.findAllByAnimeId(any()) } returns listOf(episode)
            every {
                episodeRepository.findByAnimeIdAndSeasonAndNumberAndEpisodeType(any(), any(), any(), any())
            } returns episode

            // When
            val found = episodeService.findById(requireNotNull(episode.episode.id))
            val foundByAnime = episodeService.findAllByAnimeId(requireNotNull(episode.episode.animeId))
            val foundByIdentity = episodeService.findByAnimeIdAndSeasonAndNumberAndEpisodeType(
                requireNotNull(episode.episode.animeId),
                episode.episode.season,
                episode.episode.number,
                episode.episode.episodeType
            )

            // Then
            assertEquals("Episode", found?.localizations?.single()?.title)
            assertEquals(listOf(episode), foundByAnime)
            assertSame(episode, foundByIdentity)
        }
    }
}
