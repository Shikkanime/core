package fr.shikkanime.jobs

import fr.shikkanime.database.builders.impl.AnimeMockKBuilder
import fr.shikkanime.database.services.AnimeService
import fr.shikkanime.jobs.builders.impl.PlatformAnimeLocalizationMockKBuilder
import fr.shikkanime.jobs.builders.impl.PlatformAnimeMockKBuilder
import fr.shikkanime.jobs.builders.impl.PlatformImageMockKBuilder
import fr.shikkanime.jobs.platforms.PlatformAnime
import fr.shikkanime.models.Anime
import fr.shikkanime.models.Country
import fr.shikkanime.models.ImageType
import fr.shikkanime.models.Platform
import io.mockk.every
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
@DisplayName("tests for the anime ingestion service")
class AnimeIngestionServiceTest {
    @MockK private lateinit var animeService: AnimeService

    @InjectMockKs private lateinit var ingestionService: AnimeIngestionService

    private val platformAnime = PlatformAnimeMockKBuilder {
        id = "123"
        platform = Platform.ANIMATION_DIGITAL_NETWORK
        images = listOf(
            PlatformImageMockKBuilder {
                type = ImageType.BANNER
                url = "https://example.test/banner.png"
            }.build()
        )
        localizations = listOf(
            PlatformAnimeLocalizationMockKBuilder {
                country = Country.FR
                locale = Country.FR.defaultLocale
                title = "Title"
                description = "Description"
            }.build()
        )
    }.build()

    @Nested
    @DisplayName("tests for the 'saveAll' method")
    inner class SaveAllTests {
        @Test
        fun `should save a new anime when the platform link is unknown`() {
            // Given
            every {
                animeService.findByPlatformAndPlatformId(Platform.ANIMATION_DIGITAL_NETWORK, "123")
            } returns null
            val saved = slot<Anime>()
            every { animeService.save(capture(saved)) } returnsArgument 0

            // When
            ingestionService.saveAll(Platform.ANIMATION_DIGITAL_NETWORK, listOf(platformAnime))

            // Then
            assertEquals("123", saved.captured.platforms.single().platformId)
        }

        @Test
        fun `should reuse the stored anime identifier when the platform link exists`() {
            // Given
            val existing = AnimeMockKBuilder { id = Uuid.random() }.build()
            every {
                animeService.findByPlatformAndPlatformId(Platform.ANIMATION_DIGITAL_NETWORK, "123")
            } returns existing
            val saved = slot<Anime>()
            every { animeService.save(capture(saved)) } returnsArgument 0

            // When
            ingestionService.saveAll(Platform.ANIMATION_DIGITAL_NETWORK, listOf(platformAnime))

            // Then
            assertEquals(existing.id, saved.captured.id)
        }

        @Test
        fun `should map the harvested children to the stored aggregate`() {
            // Given
            every {
                animeService.findByPlatformAndPlatformId(Platform.ANIMATION_DIGITAL_NETWORK, "123")
            } returns null
            val saved = slot<Anime>()
            every { animeService.save(capture(saved)) } returnsArgument 0

            // When
            ingestionService.saveAll(Platform.ANIMATION_DIGITAL_NETWORK, listOf(platformAnime))

            // Then
            val anime = saved.captured
            val localization = anime.localizations.single()
            assertEquals(Country.FR.defaultLocale, localization.locale)
            assertEquals("Title", localization.title)
            assertEquals("Description", localization.description)
            assertEquals(ImageType.BANNER, anime.images.single().type)
            assertEquals(Platform.ANIMATION_DIGITAL_NETWORK, anime.platforms.single().platform)
        }
    }
}
