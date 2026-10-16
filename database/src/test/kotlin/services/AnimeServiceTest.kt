package fr.shikkanime.database.services

import fr.shikkanime.core.TimeUtils
import fr.shikkanime.database.builders.impl.AnimeLocalizationMockKBuilder
import fr.shikkanime.database.builders.impl.AnimeMockKBuilder
import fr.shikkanime.database.builders.impl.AnimePlatformMockKBuilder
import fr.shikkanime.database.builders.impl.ImageMockKBuilder
import fr.shikkanime.database.repositories.AnimeLocalizationRepository
import fr.shikkanime.database.repositories.AnimePlatformRepository
import fr.shikkanime.database.repositories.AnimeRepository
import fr.shikkanime.database.repositories.ImageRepository
import fr.shikkanime.database.services.impl.AnimeServiceImpl
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

    @Nested
    @DisplayName("tests for the 'save' method")
    inner class SaveTests {
        @Test
        fun `should persist the identity and every child of the aggregate`() {
            // Given
            val anime = AnimeMockKBuilder {
                id = Uuid.random()
                createdAt = TimeUtils.now()
                updatedAt = TimeUtils.now()
                localizations = listOf(AnimeLocalizationMockKBuilder().build())
                platforms = listOf(AnimePlatformMockKBuilder().build())
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
            }.build()

            every { animeRepository.save(anime) } returns anime
            every { localizationRepository.save(anime, any()) } returns anime.localizations.single()
            every { platformRepository.save(anime, any()) } returns anime.platforms.single()
            every { imageRepository.save(any()) } returns anime.images.single()

            // When
            val saved = animeService.save(anime)

            // Then
            verify(exactly = 1) { animeRepository.save(anime) }
            verify(exactly = 1) { localizationRepository.save(anime, any()) }
            verify(exactly = 1) { platformRepository.save(anime, any()) }
            verify(exactly = 1) { imageRepository.save(any()) }
            assertEquals(anime.id, saved.id)
        }

        @Test
        fun `should store images under the anime identifier whatever owner the image carries`() {
            // Given
            val anime = AnimeMockKBuilder {
                id = Uuid.random()
                createdAt = TimeUtils.now()
                updatedAt = TimeUtils.now()
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
            }.build()
            val stored = slot<Image>()

            every { animeRepository.save(anime) } returns anime
            every { imageRepository.save(capture(stored)) } returns anime.images.single()

            // When
            animeService.save(anime)

            // Then
            assertEquals(anime.id, stored.captured.entityId)
        }
    }

    @Nested
    @DisplayName("tests for the 'findById' method")
    inner class FindByIdTests {
        @Test
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
        fun `should return the aggregate read from the anime repository`() {
            // Given
            val anime = AnimeMockKBuilder {
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
                    ImageMockKBuilder { type = ImageType.BANNER }
                        .build()
                )
            }.build()
            every { animeRepository.findModelById(any()) } returns anime

            // When
            val found = animeService.findById(requireNotNull(anime.id))

            // Then
            assertEquals("One Piece", found?.localizations?.single()?.title)
        }
    }
}
