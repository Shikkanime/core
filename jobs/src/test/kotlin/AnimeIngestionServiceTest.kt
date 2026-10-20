package fr.shikkanime.jobs

import fr.shikkanime.database.builders.impl.AnimeMockKBuilder
import fr.shikkanime.database.builders.impl.AnimePlatformMockKBuilder
import fr.shikkanime.database.builders.impl.AnimeWithChildrenMockKBuilder
import fr.shikkanime.database.services.AnimeService
import fr.shikkanime.jobs.builders.impl.PlatformAnimeLocalizationMockKBuilder
import fr.shikkanime.jobs.builders.impl.PlatformAnimeMockKBuilder
import fr.shikkanime.jobs.builders.impl.PlatformEpisodeLocalizationMockKBuilder
import fr.shikkanime.jobs.builders.impl.PlatformEpisodeMockKBuilder
import fr.shikkanime.jobs.builders.impl.PlatformImageMockKBuilder
import fr.shikkanime.jobs.platforms.PlatformAnime
import fr.shikkanime.jobs.platforms.PlatformEpisode
import fr.shikkanime.models.AnimeWithChildren
import fr.shikkanime.models.Country
import fr.shikkanime.models.EpisodeType
import fr.shikkanime.models.ImageType
import fr.shikkanime.models.Locale
import fr.shikkanime.models.Platform
import fr.shikkanime.models.canonicalTitle
import io.mockk.every
import io.mockk.verify
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.slot
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.extension.ExtendWith
import java.time.ZonedDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.uuid.Uuid

@ExtendWith(MockKExtension::class)
@DisplayName("tests for the anime ingestion service")
class AnimeIngestionServiceTest {
    @MockK private lateinit var animeService: AnimeService

    @InjectMockKs private lateinit var ingestionService: AnimeIngestionService

    /**
     * Builds a harvested anime exactly as a streaming platform produces it.
     *
     * Every field is stubbed: the ingestion derives its content keys from them, and an unstubbed
     * mock value would make two equal builds look like two different harvests.
     *
     * @param id identifier of the anime on the streaming platform.
     * @param title localized title of the harvested anime.
     * @param description localized description of the harvested anime.
     * @param platform streaming platform the anime is harvested from.
     * @return the platform anime fixture.
     * @see [PlatformAnimeMockKBuilder]
     */
    private fun platformAnime(
        id: String = "123",
        title: String = "Title",
        description: String = "Description",
        platform: Platform = Platform.ANIMATION_DIGITAL_NETWORK
    ): PlatformAnime =
        PlatformAnimeMockKBuilder {
            this.id = id
            this.platform = platform
            normalizedTitle = canonicalTitle(mapOf(Country.FR.defaultLocale to title))
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
                    this.title = title
                    this.description = description
                }.build()
            )
        }.build()

    /**
     * Builds a harvested episode carrying the given anime, with every field stubbed.
     *
     * The ingestion derives the content keys of the harvest from all these fields: an unstubbed
     * mock value would make two equal builds look like two different harvests.
     *
     * @param anime harvested anime carried by the episode.
     * @param id identifier of the episode on the streaming platform.
     * @param description localized description carried by the episode.
     * @return the platform episode fixture.
     * @see [PlatformEpisodeMockKBuilder]
     */
    private fun episode(
        anime: PlatformAnime = platformAnime(),
        id: String = "1001",
        description: String = "Description"
    ): PlatformEpisode =
        PlatformEpisodeMockKBuilder {
            this.anime = anime
            this.id = id
            image = "https://example.test/episode.jpg"
            releaseDateTime = ZonedDateTime.parse("2026-10-20T10:00:00Z")
            season = 1
            number = 1
            episodeType = EpisodeType.EPISODE
            duration = 1440
            url = "https://example.test/video"
            audioLocales = listOf(Locale.JA_JP)
            uncensored = false
            localizations = listOf(
                PlatformEpisodeLocalizationMockKBuilder {
                    country = Country.FR
                    title = "Episode 1"
                    this.description = description
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
            ingestionService.saveAll(listOf(episode()))

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
            ingestionService.saveAll(listOf(episode()))

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

            val lowerPriority = platformAnime(platform = Platform.CRUNCHYROLL)

            // When
            ingestionService.saveAll(listOf(episode(anime = lowerPriority)))

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
            ingestionService.saveAll(listOf(episode()))

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
        @DisplayName("should skip the save workflow when the harvest content is unchanged")
        fun `should skip the save workflow when the harvest content is unchanged`() {
            // Given
            every { animeService.findAllByNormalizedTitles(any()) } returns emptyMap()
            every { animeService.save(any()) } returnsArgument 0
            val harvested = episode()

            // When
            val first = ingestionService.saveAll(listOf(harvested))
            val second = ingestionService.saveAll(listOf(harvested))

            // Then: the second run enters no read and no write
            assertEquals(1, first)
            assertEquals(0, second)
            verify(exactly = 1) { animeService.findAllByNormalizedTitles(any()) }
            verify(exactly = 1) { animeService.save(any()) }
        }

        @Test
        @DisplayName("should skip the save workflow when the same content comes in new instances")
        fun `should skip the save workflow when the same content comes in new instances`() {
            // Given
            every { animeService.findAllByNormalizedTitles(any()) } returns emptyMap()
            every { animeService.save(any()) } returnsArgument 0

            // When: two harvests carrying the same content as distinct instances
            val first = ingestionService.saveAll(listOf(episode()))
            val second = ingestionService.saveAll(listOf(episode()))

            // Then: equal content is not a change
            assertEquals(1, first)
            assertEquals(0, second)
            verify(exactly = 1) { animeService.findAllByNormalizedTitles(any()) }
            verify(exactly = 1) { animeService.save(any()) }
        }

        @Test
        @DisplayName("should skip the save workflow when the anime comes in a new but equal instance")
        fun `should skip the save workflow when the anime comes in a new but equal instance`() {
            // Given
            every { animeService.findAllByNormalizedTitles(any()) } returns emptyMap()
            every { animeService.save(any()) } returnsArgument 0

            // When: the carried anime is rebuilt with the same content
            val first = ingestionService.saveAll(listOf(episode()))
            val second = ingestionService.saveAll(listOf(episode(anime = platformAnime())))

            // Then: the anime graph is part of the content, but its content did not change
            assertEquals(1, first)
            assertEquals(0, second)
            verify(exactly = 1) { animeService.findAllByNormalizedTitles(any()) }
            verify(exactly = 1) { animeService.save(any()) }
        }

        @Test
        @DisplayName("should skip the save workflow when the same harvest content arrives in another order")
        fun `should skip the save workflow when the same harvest content arrives in another order`() {
            // Given
            every { animeService.findAllByNormalizedTitles(any()) } returns emptyMap()
            every { animeService.save(any()) } returnsArgument 0
            val first = episode()
            val second = episode(
                anime = platformAnime(id = "456", title = "Other", description = "Other description"),
                id = "1002"
            )

            // When
            val firstRun = ingestionService.saveAll(listOf(first, second))
            val secondRun = ingestionService.saveAll(listOf(second, first))

            // Then: one run total, one anime saved per harvested title
            assertEquals(2, firstRun)
            assertEquals(0, secondRun)
            verify(exactly = 1) { animeService.findAllByNormalizedTitles(any()) }
            verify(exactly = 2) { animeService.save(any()) }
        }

        @Test
        @DisplayName("should skip the save workflow when an episode comes duplicated in the harvest")
        fun `should skip the save workflow when an episode comes duplicated in the harvest`() {
            // Given
            every { animeService.findAllByNormalizedTitles(any()) } returns emptyMap()
            every { animeService.save(any()) } returnsArgument 0
            val harvested = episode()

            // When: the caller maps one entry per episode, so one anime can appear twice
            val first = ingestionService.saveAll(listOf(harvested, harvested))
            val second = ingestionService.saveAll(listOf(harvested))

            // Then
            assertEquals(1, first)
            assertEquals(0, second)
            verify(exactly = 1) { animeService.findAllByNormalizedTitles(any()) }
            verify(exactly = 1) { animeService.save(any()) }
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

        @Test
        @DisplayName("should run the save workflow again when the harvest content changed")
        fun `should run the save workflow again when the harvest content changed`() {
            // Given
            every { animeService.findAllByNormalizedTitles(any()) } returns emptyMap()
            every { animeService.save(any()) } returnsArgument 0

            // When
            val first = ingestionService.saveAll(listOf(episode()))
            val second = ingestionService.saveAll(listOf(episode(id = "1002")))

            // Then: the new content goes through the whole workflow
            assertEquals(1, first)
            assertEquals(1, second)
            verify(exactly = 2) { animeService.findAllByNormalizedTitles(any()) }
            verify(exactly = 2) { animeService.save(any()) }
        }

        @Test
        @DisplayName("should run the save workflow again when only an episode field changed")
        fun `should run the save workflow again when only an episode field changed`() {
            // Given
            every { animeService.findAllByNormalizedTitles(any()) } returns emptyMap()
            every { animeService.save(any()) } returnsArgument 0

            // When: the same episode comes back with a new description
            val first = ingestionService.saveAll(listOf(episode()))
            val second = ingestionService.saveAll(listOf(episode(description = "Changed")))

            // Then: the identifier alone does not make the content
            assertEquals(1, first)
            assertEquals(1, second)
            verify(exactly = 2) { animeService.save(any()) }
        }

        @Test
        @DisplayName("should run the save workflow again when only the anime content changed")
        fun `should run the save workflow again when only the anime content changed`() {
            // Given
            every { animeService.findAllByNormalizedTitles(any()) } returns emptyMap()
            every { animeService.save(any()) } returnsArgument 0

            // When: the same episode comes back with a new anime description
            val first = ingestionService.saveAll(listOf(episode()))
            val second = ingestionService.saveAll(
                listOf(episode(anime = platformAnime(description = "Changed")))
            )

            // Then: the carried anime graph is part of the content
            assertEquals(1, first)
            assertEquals(1, second)
            verify(exactly = 2) { animeService.save(any()) }
        }

        @Test
        @DisplayName("should run the save workflow again when the previous run failed")
        fun `should run the save workflow again when the previous run failed`() {
            // Given
            every { animeService.findAllByNormalizedTitles(any()) } returns emptyMap()
            every { animeService.save(any()) } throws IllegalStateException("boom")
            val harvested = episode()

            // When: the first run fails before completing, the same harvest comes back
            assertFailsWith<IllegalStateException> { ingestionService.saveAll(listOf(harvested)) }
            every { animeService.save(any()) } returnsArgument 0
            val retried = ingestionService.saveAll(listOf(harvested))

            // Then: the failed run is retried
            assertEquals(1, retried)
            verify(exactly = 2) { animeService.save(any()) }
        }
    }
}
