package fr.shikkanime.database.services

import fr.shikkanime.core.TimeUtils
import fr.shikkanime.database.builders.impl.EpisodeLocalizationMockKBuilder
import fr.shikkanime.database.builders.impl.EpisodeMockKBuilder
import fr.shikkanime.database.builders.impl.EpisodePlatformMockKBuilder
import fr.shikkanime.database.builders.impl.EpisodeWithChildrenMockKBuilder
import fr.shikkanime.database.builders.impl.ImageMockKBuilder
import fr.shikkanime.database.repositories.EpisodeLocalizationRepository
import fr.shikkanime.database.repositories.EpisodePlatformRepository
import fr.shikkanime.database.repositories.EpisodeRepository
import fr.shikkanime.database.repositories.ImageRepository
import fr.shikkanime.database.services.impl.EpisodeServiceImpl
import fr.shikkanime.models.EpisodeLocalization
import fr.shikkanime.models.EpisodePlatform
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
    @MockK private lateinit var platformRepository: EpisodePlatformRepository
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

    /**
     * Stubs the identity resolution of a save with no stored episode.
     *
     * @see [EpisodeRepository.findByPlatformAndPlatformId]
     */
    private fun stubNoStoredEpisode() {
        every { episodeRepository.findByPlatformAndPlatformId(any(), any()) } returns null
        every {
            episodeRepository.findByAnimeIdAndSeasonAndNumberAndEpisodeType(any(), any(), any(), any())
        } returns null
    }

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
                platforms = listOf(
                    EpisodePlatformMockKBuilder {
                        id = Uuid.random()
                        createdAt = TimeUtils.now()
                        updatedAt = TimeUtils.now()
                        platform = Platform.ANIMATION_DIGITAL_NETWORK
                        platformId = "3001"
                        url = "https://example.test/episodes/3001"
                        audioLocales = setOf(Locale.FR_FR)
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
            every { platformRepository.save(any()) } returns episode.platforms.single()
            every { imageRepository.save(any()) } returns episode.images.single()

            // When
            val saved = episodeService.save(episode)

            // Then
            verify(exactly = 1) { episodeRepository.save(episode) }
            verify(exactly = 1) { localizationRepository.save(any()) }
            verify(exactly = 1) { platformRepository.save(any()) }
            verify(exactly = 1) { imageRepository.save(any()) }
            assertEquals(episode.episode.id, saved.episode.id)
        }

        @Test
        @DisplayName("should store images under the episode identifier whatever owner the image carries")
        fun `should store images under the episode identifier whatever owner the image carries`() {
            // Given
            val episode = episodeWithChildrenFixture {
                localizations = emptyList()
                platforms = emptyList()
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
                platforms = listOf(
                    EpisodePlatformMockKBuilder {
                        id = Uuid.random()
                        episodeId = Uuid.random()
                        platform = Platform.ANIMATION_DIGITAL_NETWORK
                        platformId = "3001"
                        url = "https://example.test/episodes/3001"
                        audioLocales = setOf(Locale.FR_FR)
                        uncensored = false
                        releaseDateTime = TimeUtils.now()
                    }.build()
                )
                images = emptyList()
            }
            val storedLocalization = slot<EpisodeLocalization>()
            val storedPlatform = slot<EpisodePlatform>()

            every { episodeRepository.save(episode) } returns episode.episode
            every { localizationRepository.save(capture(storedLocalization)) } returns episode.localizations.single()
            every { platformRepository.save(capture(storedPlatform)) } returns episode.platforms.single()

            // When
            episodeService.save(episode)

            // Then
            assertEquals(episode.episode.id, storedLocalization.captured.episodeId)
            assertEquals(episode.episode.id, storedPlatform.captured.episodeId)
        }

        @Test
        @DisplayName("should reuse the stored episode carrying a platform release")
        fun `should reuse the stored episode carrying a platform release`() {
            // Given
            val stored = episodeWithChildrenFixture {
                localizations = emptyList()
                platforms = emptyList()
                images = emptyList()
            }
            val incoming = EpisodeWithChildrenMockKBuilder {
                episode = EpisodeMockKBuilder {
                    animeId = stored.episode.animeId
                    season = 1
                    number = 1
                    episodeType = EpisodeType.EPISODE
                    releaseDateTime = TimeUtils.now()
                    duration = 1440
                }.build()
                localizations = emptyList()
                platforms = listOf(
                    EpisodePlatformMockKBuilder {
                        platform = Platform.ANIMATION_DIGITAL_NETWORK
                        platformId = "3001"
                        url = "https://example.test/episodes/3001"
                        audioLocales = setOf(Locale.FR_FR)
                        uncensored = false
                        releaseDateTime = TimeUtils.now()
                    }.build()
                )
                images = emptyList()
            }.build()
            val slot = slot<EpisodeWithChildren>()

            every {
                episodeRepository.findByPlatformAndPlatformId(any(), "3001")
            } returns stored
            every { episodeRepository.save(capture(slot)) } returns stored.episode
            every { platformRepository.save(any()) } returns incoming.platforms.single()

            // When
            episodeService.save(incoming)

            // Then
            assertEquals(stored.episode.id, slot.captured.episode.id)
        }

        @Test
        @DisplayName("should reuse the stored episode carrying the identity when no release is known")
        fun `should reuse the stored episode carrying the identity when no release is known`() {
            // Given
            val stored = episodeWithChildrenFixture {
                localizations = emptyList()
                platforms = emptyList()
                images = emptyList()
            }
            val incoming = EpisodeWithChildrenMockKBuilder {
                episode = EpisodeMockKBuilder {
                    animeId = stored.episode.animeId
                    season = 1
                    number = 1
                    episodeType = EpisodeType.EPISODE
                    releaseDateTime = TimeUtils.now()
                    duration = 1440
                }.build()
                localizations = emptyList()
                platforms = emptyList()
                images = emptyList()
            }.build()
            val slot = slot<EpisodeWithChildren>()

            stubNoStoredEpisode()
            every {
                episodeRepository.findByAnimeIdAndSeasonAndNumberAndEpisodeType(any(), any(), any(), any())
            } returns stored
            every { episodeRepository.save(capture(slot)) } returns stored.episode

            // When
            episodeService.save(incoming)

            // Then
            assertEquals(stored.episode.id, slot.captured.episode.id)
        }

        @Test
        @DisplayName("should create a new episode when the known release belongs to another anime")
        fun `should create a new episode when the known release belongs to another anime`() {
            // Given
            val otherAnime = episodeWithChildrenFixture {
                localizations = emptyList()
                platforms = emptyList()
                images = emptyList()
            }
            val incoming = EpisodeWithChildrenMockKBuilder {
                episode = EpisodeMockKBuilder {
                    animeId = Uuid.random()
                    season = 1
                    number = 1
                    episodeType = EpisodeType.EPISODE
                    releaseDateTime = TimeUtils.now()
                    duration = 1440
                }.build()
                localizations = emptyList()
                platforms = listOf(
                    EpisodePlatformMockKBuilder {
                        platform = Platform.ANIMATION_DIGITAL_NETWORK
                        platformId = "3001"
                        url = "https://example.test/episodes/3001"
                        audioLocales = setOf(Locale.FR_FR)
                        uncensored = false
                        releaseDateTime = TimeUtils.now()
                    }.build()
                )
                images = emptyList()
            }.build()
            val slot = slot<EpisodeWithChildren>()

            stubNoStoredEpisode()
            every { episodeRepository.findByPlatformAndPlatformId(any(), "3001") } returns otherAnime
            every { episodeRepository.save(capture(slot)) } returns otherAnime.episode
            every { platformRepository.save(any()) } returns incoming.platforms.single()

            // When
            episodeService.save(incoming)

            // Then
            assertNull(slot.captured.episode.id)
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
                platforms = emptyList()
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
