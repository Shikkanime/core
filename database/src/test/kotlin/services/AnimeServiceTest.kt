package fr.shikkanime.database.services

import fr.shikkanime.core.TimeUtils
import fr.shikkanime.database.builders.impl.AnimeLocalizationMockKBuilder
import fr.shikkanime.database.builders.impl.AnimeMockKBuilder
import fr.shikkanime.database.builders.impl.AnimePlatformMockKBuilder
import fr.shikkanime.database.builders.impl.AnimeWithChildrenMockKBuilder
import fr.shikkanime.database.builders.impl.ImageMockKBuilder
import fr.shikkanime.database.repositories.AnimeLocalizationRepository
import fr.shikkanime.database.repositories.AnimePlatformRepository
import fr.shikkanime.database.repositories.AnimeRepository
import fr.shikkanime.database.repositories.ImageRepository
import fr.shikkanime.database.services.impl.AnimeServiceImpl
import fr.shikkanime.models.AnimeLocalization
import fr.shikkanime.models.AnimePlatform
import fr.shikkanime.models.AnimeWithChildren
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
import kotlin.uuid.Uuid

@ExtendWith(MockKExtension::class)
@DisplayName("tests for the anime service")
class AnimeServiceTest {
    @MockK private lateinit var animeRepository: AnimeRepository
    @MockK private lateinit var localizationRepository: AnimeLocalizationRepository
    @MockK private lateinit var platformRepository: AnimePlatformRepository
    @MockK private lateinit var imageRepository: ImageRepository

    @InjectMockKs private lateinit var animeService: AnimeServiceImpl

    /**
     * Builds the aggregate fixture around the canonical root anime of these tests.
     *
     * @param configuration lambda receiving the builder to set the children of the aggregate.
     * @return the aggregate fixture around the "One Piece" root anime.
     * @see [AnimeWithChildrenMockKBuilder]
     */
    private fun animeWithChildrenFixture(
        configuration: AnimeWithChildrenMockKBuilder.() -> Unit
    ): AnimeWithChildren =
        AnimeWithChildrenMockKBuilder {
            anime = AnimeMockKBuilder {
                id = Uuid.random()
                normalizedTitle = "One Piece"
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
            val anime = animeWithChildrenFixture {
                localizations = listOf(
                    AnimeLocalizationMockKBuilder {
                        id = Uuid.random()
                        createdAt = TimeUtils.now()
                        updatedAt = TimeUtils.now()
                        locale = Locale.FR_FR
                        title = "One Piece"
                        description = "Description"
                    }.build()
                )
                platforms = listOf(
                    AnimePlatformMockKBuilder {
                        id = Uuid.random()
                        createdAt = TimeUtils.now()
                        updatedAt = TimeUtils.now()
                        platform = Platform.ANIMATION_DIGITAL_NETWORK
                        platformId = "123"
                    }.build()
                )
                images = listOf(
                    ImageMockKBuilder {
                        id = Uuid.random()
                        createdAt = TimeUtils.now()
                        updatedAt = TimeUtils.now()
                        entityId = Uuid.random()
                        type = ImageType.THUMBNAIL
                        url = "https://a"
                    }
                        .build()
                )
            }
            val root = anime.anime

            every { animeRepository.save(anime) } returns root
            every { localizationRepository.save(any()) } returns anime.localizations.single()
            every { platformRepository.save(any()) } returns anime.platforms.single()
            every { imageRepository.save(any()) } returns anime.images.single()

            // When
            val saved = animeService.save(anime)

            // Then
            verify(exactly = 1) { animeRepository.save(anime) }
            verify(exactly = 1) { localizationRepository.save(any()) }
            verify(exactly = 1) { platformRepository.save(any()) }
            verify(exactly = 1) { imageRepository.save(any()) }
            assertEquals(anime.anime.id, saved.anime.id)
        }

        @Test
        @DisplayName("should store images under the anime identifier whatever owner the image carries")
        fun `should store images under the anime identifier whatever owner the image carries`() {
            // Given
            val anime = animeWithChildrenFixture {
                localizations = emptyList()
                platforms = emptyList()
                images = listOf(
                    ImageMockKBuilder {
                        id = Uuid.random()
                        createdAt = TimeUtils.now()
                        updatedAt = TimeUtils.now()
                        type = ImageType.TITLE
                        entityId = Uuid.random()
                        url = "https://title"
                    }.build()
                )
            }
            val root = anime.anime
            val stored = slot<Image>()

            every { animeRepository.save(anime) } returns root
            every { imageRepository.save(capture(stored)) } returns anime.images.single()

            // When
            animeService.save(anime)

            // Then
            assertEquals(anime.anime.id, stored.captured.entityId)
        }

        @Test
        @DisplayName("should store children under the anime identifier whatever owner the child carries")
        fun `should store children under the anime identifier whatever owner the child carries`() {
            // Given
            val anime = animeWithChildrenFixture {
                localizations = listOf(
                    AnimeLocalizationMockKBuilder {
                        id = Uuid.random()
                        createdAt = TimeUtils.now()
                        updatedAt = TimeUtils.now()
                        animeId = Uuid.random()
                        locale = Locale.FR_FR
                        title = "One Piece"
                        description = "Description"
                    }.build()
                )
                platforms = listOf(
                    AnimePlatformMockKBuilder {
                        id = Uuid.random()
                        createdAt = TimeUtils.now()
                        updatedAt = TimeUtils.now()
                        animeId = Uuid.random()
                        platform = Platform.ANIMATION_DIGITAL_NETWORK
                        platformId = "123"
                    }.build()
                )
                images = emptyList()
            }
            val root = anime.anime
            val localization = anime.localizations.single()
            val animePlatform = anime.platforms.single()
            val storedLocalization = slot<AnimeLocalization>()
            val storedPlatform = slot<AnimePlatform>()

            every { animeRepository.save(anime) } returns root
            every { localizationRepository.save(capture(storedLocalization)) } returns localization
            every { platformRepository.save(capture(storedPlatform)) } returns animePlatform

            // When
            animeService.save(anime)

            // Then
            assertEquals(anime.anime.id, storedLocalization.captured.animeId)
            assertEquals(anime.anime.id, storedPlatform.captured.animeId)
        }
    }

    @Nested
    @DisplayName("tests for the 'findById' method")
    inner class FindByIdTests {
        @Test
        @DisplayName("should return null when the anime does not exist")
        fun `should return null when the anime does not exist`() {
            // Given
            val unknown = Uuid.random()
            every { animeRepository.findModelById(unknown) } returns null

            // When
            val found = animeService.findById(unknown)

            // Then
            assertNull(found)
        }

        @Test
        @DisplayName("should return the aggregate read from the anime repository")
        fun `should return the aggregate read from the anime repository`() {
            // Given
            val anime = animeWithChildrenFixture {
                localizations = listOf(
                    AnimeLocalizationMockKBuilder {
                        id = Uuid.random()
                        locale = Locale.FR_FR
                        title = "One Piece"
                    }.build()
                )
                platforms = listOf(
                    AnimePlatformMockKBuilder {
                        id = Uuid.random()
                        platform = Platform.ANIMATION_DIGITAL_NETWORK
                        platformId = "123"
                    }.build()
                )
                images = listOf(
                    ImageMockKBuilder {
                        id = Uuid.random()
                        createdAt = TimeUtils.now()
                        updatedAt = TimeUtils.now()
                        entityId = Uuid.random()
                        type = ImageType.BANNER
                        url = "https://b"
                    }.build()
                )
            }
            every { animeRepository.findModelById(any()) } returns anime

            // When
            val found = animeService.findById(requireNotNull(anime.anime.id))

            // Then
            assertEquals("One Piece", found?.localizations?.single()?.title)
        }
    }
}
