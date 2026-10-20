package fr.shikkanime.jobs

import fr.shikkanime.database.builders.impl.AnimeMockKBuilder
import fr.shikkanime.database.builders.impl.AnimePlatformMockKBuilder
import fr.shikkanime.database.builders.impl.AnimeWithChildrenMockKBuilder
import fr.shikkanime.database.services.AnimeService
import fr.shikkanime.jobs.builders.harvest
import fr.shikkanime.jobs.builders.harvestedAnime
import fr.shikkanime.jobs.builders.harvestedEpisode
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
            ingestionService.saveAll(listOf(harvest(true, harvestedEpisode(harvestedAnime()))))

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
            ingestionService.saveAll(listOf(harvest(true, harvestedEpisode(harvestedAnime()))))

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

            val lowerPriority = harvestedAnime(platform = Platform.CRUNCHYROLL)

            // When
            ingestionService.saveAll(listOf(harvest(true, harvestedEpisode(lowerPriority))))

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
            ingestionService.saveAll(listOf(harvest(true, harvestedEpisode(harvestedAnime()))))

            // Then
            val anime = saved.captured
            val localization = anime.localizations.single()
            assertEquals(Country.FR.defaultLocale, localization.locale)
            assertEquals("Title", localization.title)
            assertEquals("Description", localization.description)
            assertEquals(ImageType.BANNER, anime.images.single().type)
            assertEquals(Platform.ANIMATION_DIGITAL_NETWORK, anime.platforms.single().platform)
        }

        @Test
        @DisplayName("should skip the save workflow when no platform reports a change")
        fun `should skip the save workflow when no platform reports a change`() {
            // Given
            every { animeService.findAllByNormalizedTitles(any()) } returns emptyMap()
            every { animeService.save(any()) } returnsArgument 0

            // When
            val saved = ingestionService.saveAll(listOf(harvest(false, harvestedEpisode(harvestedAnime()))))

            // Then: the workflow enters no read and no write
            assertEquals(0, saved)
            verify(exactly = 0) { animeService.findAllByNormalizedTitles(any()) }
            verify(exactly = 0) { animeService.save(any()) }
        }

        @Test
        @DisplayName("should run the save workflow when one platform reports a change")
        fun `should run the save workflow when one platform reports a change`() {
            // Given
            every { animeService.findAllByNormalizedTitles(any()) } returns emptyMap()
            every { animeService.save(any()) } returnsArgument 0
            val quiet = harvest(false, harvestedEpisode(harvestedAnime()))
            val changed = harvest(
                true,
                harvestedEpisode(anime = harvestedAnime(id = "456", title = "Other"), id = "1002")
            )

            // When
            val saved = ingestionService.saveAll(listOf(quiet, changed))

            // Then: one changed platform is enough, and the whole pooled harvest goes through
            assertEquals(2, saved)
            verify(exactly = 1) { animeService.findAllByNormalizedTitles(any()) }
            verify(exactly = 2) { animeService.save(any()) }
        }

        @Test
        @DisplayName("should run the save workflow on every reported change")
        fun `should run the save workflow on every reported change`() {
            // Given
            every { animeService.findAllByNormalizedTitles(any()) } returns emptyMap()
            every { animeService.save(any()) } returnsArgument 0

            // When: the platform reports a change, then nothing, then a change again
            val first = ingestionService.saveAll(listOf(harvest(true, harvestedEpisode(harvestedAnime()))))
            val second = ingestionService.saveAll(listOf(harvest(false, harvestedEpisode(harvestedAnime()))))
            val third = ingestionService.saveAll(listOf(harvest(true, harvestedEpisode(harvestedAnime()))))

            // Then
            assertEquals(1, first)
            assertEquals(0, second)
            assertEquals(1, third)
            verify(exactly = 2) { animeService.save(any()) }
        }

        @Test
        @DisplayName("should skip a harvest that reports a change but carries no episode")
        fun `should skip a harvest that reports a change but carries no episode`() {
            // Given
            every { animeService.findAllByNormalizedTitles(any()) } returns emptyMap()
            every { animeService.save(any()) } returnsArgument 0

            // When
            val saved = ingestionService.saveAll(listOf(harvest(true)))

            // Then: an emptied payload has nothing to persist, the persistence is not even read
            assertEquals(0, saved)
            verify(exactly = 0) { animeService.findAllByNormalizedTitles(any()) }
            verify(exactly = 0) { animeService.save(any()) }
        }

        @Test
        @DisplayName("should skip an empty harvest without touching the persistence")
        fun `should skip an empty harvest without touching the persistence`() {
            // Given
            every { animeService.findAllByNormalizedTitles(any()) } returns emptyMap()
            every { animeService.save(any()) } returnsArgument 0

            // When
            val saved = ingestionService.saveAll(emptyList())

            // Then: nothing to persist, the persistence is not even read
            assertEquals(0, saved)
            verify(exactly = 0) { animeService.findAllByNormalizedTitles(any()) }
            verify(exactly = 0) { animeService.save(any()) }
        }
    }
}