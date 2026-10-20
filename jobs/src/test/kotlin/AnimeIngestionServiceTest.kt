package fr.shikkanime.jobs

import fr.shikkanime.database.builders.impl.AnimeMockKBuilder
import fr.shikkanime.database.builders.impl.AnimePlatformMockKBuilder
import fr.shikkanime.database.builders.impl.AnimeWithChildrenMockKBuilder
import fr.shikkanime.database.services.AnimeService
import fr.shikkanime.jobs.builders.impl.PlatformAnimeLocalizationMockKBuilder
import fr.shikkanime.jobs.builders.impl.PlatformAnimeMockKBuilder
import fr.shikkanime.jobs.builders.impl.PlatformImageMockKBuilder
import fr.shikkanime.models.AnimeWithChildren
import fr.shikkanime.models.Country
import fr.shikkanime.models.ImageType
import fr.shikkanime.models.Platform
import io.mockk.every
import io.mockk.verify
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
        normalizedTitle = "Title"
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
        @DisplayName("should save a new anime when no stored anime carries the identity name")
        fun `should save a new anime when no stored anime carries the identity name`() {
            // Given
            every { animeService.findAllByNormalizedTitles(any()) } returns emptyMap()
            val saved = slot<AnimeWithChildren>()
            every { animeService.save(capture(saved)) } returnsArgument 0

            // When
            ingestionService.saveAll(listOf(platformAnime))

            // Then
            assertEquals("123", saved.captured.platforms.single().platformId)
        }

        @Test
        @DisplayName("should reuse the stored anime identifier when one carries the identity name")
        fun `should reuse the stored anime identifier when one carries the identity name`() {
            // Given
            val existing = AnimeWithChildrenMockKBuilder {
                anime = AnimeMockKBuilder { id = Uuid.random() }.build()
                platforms = listOf(
                    AnimePlatformMockKBuilder { platform = Platform.ANIMATION_DIGITAL_NETWORK }.build()
                )
            }.build()
            every { animeService.findAllByNormalizedTitles(any()) } returns mapOf("Title" to existing)
            val saved = slot<AnimeWithChildren>()
            every { animeService.save(capture(saved)) } returnsArgument 0

            // When
            ingestionService.saveAll(listOf(platformAnime))

            // Then
            assertEquals(existing.anime.id, saved.captured.anime.id)
        }

        @Test
        @DisplayName("should skip a harvest from a lower priority source than the stored one")
        fun `should skip a harvest from a lower priority source than the stored one`() {
            // Given: an anime already stored from the highest priority source
            val existing = AnimeWithChildrenMockKBuilder {
                anime = AnimeMockKBuilder { id = Uuid.random() }.build()
                platforms = listOf(
                    AnimePlatformMockKBuilder { platform = Platform.ANIMATION_DIGITAL_NETWORK }.build()
                )
            }.build()
            every { animeService.findAllByNormalizedTitles(any()) } returns mapOf("Title" to existing)
            every { animeService.save(any()) } returnsArgument 0

            val lowerPriority = PlatformAnimeMockKBuilder {
                platform = Platform.CRUNCHYROLL
                normalizedTitle = "Title"
            }.build()

            // When
            ingestionService.saveAll(listOf(lowerPriority))

            // Then: nothing is saved, the higher priority source keeps the data
            verify(exactly = 0) { animeService.save(any()) }
        }

        @Test
        @DisplayName("should map the harvested children to the stored aggregate")
        fun `should map the harvested children to the stored aggregate`() {
            // Given
            every { animeService.findAllByNormalizedTitles(any()) } returns emptyMap()
            val saved = slot<AnimeWithChildren>()
            every { animeService.save(capture(saved)) } returnsArgument 0

            // When
            ingestionService.saveAll(listOf(platformAnime))

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
