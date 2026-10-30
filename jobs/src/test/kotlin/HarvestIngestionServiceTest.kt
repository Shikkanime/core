package fr.shikkanime.jobs

import fr.shikkanime.core.TimeUtils
import fr.shikkanime.database.builders.impl.AnimeMockKBuilder
import fr.shikkanime.database.builders.impl.AnimePlatformMockKBuilder
import fr.shikkanime.database.builders.impl.AnimeWithChildrenMockKBuilder
import fr.shikkanime.database.builders.impl.EpisodeMockKBuilder
import fr.shikkanime.database.builders.impl.EpisodePlatformMockKBuilder
import fr.shikkanime.database.builders.impl.EpisodeWithChildrenMockKBuilder
import fr.shikkanime.database.services.AnimeService
import fr.shikkanime.database.services.EpisodeService
import fr.shikkanime.jobs.builders.harvest
import fr.shikkanime.jobs.builders.harvestedAnime
import fr.shikkanime.jobs.builders.harvestedEpisode
import fr.shikkanime.models.AnimeWithChildren
import fr.shikkanime.models.Country
import fr.shikkanime.models.EpisodeType
import fr.shikkanime.models.EpisodeWithChildren
import fr.shikkanime.models.ImageType
import fr.shikkanime.models.Platform
import io.mockk.every
import io.mockk.verify
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.slot
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.extension.ExtendWith
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.uuid.Uuid

@ExtendWith(MockKExtension::class)
@DisplayName("tests for the harvest ingestion service")
class HarvestIngestionServiceTest {
    @MockK private lateinit var animeService: AnimeService
    @MockK private lateinit var episodeService: EpisodeService

    @InjectMockKs private lateinit var ingestionService: HarvestIngestionService

    private val persistedAnimeId = Uuid.random()

    @BeforeEach
    fun setUp() {
        // By default the harvested episode is unknown to the persistence
        every { episodeService.findByPlatformAndPlatformId(any(), any()) } returns null
        every {
            episodeService.findByAnimeIdAndSeasonAndNumberAndEpisodeType(any(), any(), any(), any())
        } returns null
    }

    /**
     * Returns the aggregate as the persistence would return it: with the identifier of the saved row.
     *
     * @param anime aggregate passed to the persistence.
     * @return the aggregate carrying the identifier of the row just persisted.
     * @see [AnimeWithChildren]
     */
    private fun stored(anime: AnimeWithChildren): AnimeWithChildren =
        anime.copy(anime = anime.anime.copy(id = anime.anime.id ?: persistedAnimeId))

    /**
     * Builds a stored episode already published by the given platform.
     *
     * @param sourcePlatform streaming platform owning the stored release.
     * @return the stored episode aggregate.
     * @see [EpisodeWithChildren]
     */
    private fun storedEpisode(sourcePlatform: Platform): EpisodeWithChildren =
        EpisodeWithChildrenMockKBuilder {
            episode = EpisodeMockKBuilder {
                id = Uuid.random()
                animeId = Uuid.random()
                season = 1
                number = 1
                episodeType = EpisodeType.EPISODE
                releaseDateTime = TimeUtils.now()
                duration = 1200
            }.build()
            platforms = listOf(
                EpisodePlatformMockKBuilder {
                    platform = sourcePlatform
                    platformId = "stored-${sourcePlatform.name}"
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
            every { episodeService.save(any()) } returns EpisodeWithChildrenMockKBuilder().build()
            val saved = slot<AnimeWithChildren>()
            every { animeService.save(capture(saved)) } answers { stored(firstArg()) }

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
            every { episodeService.save(any()) } returns EpisodeWithChildrenMockKBuilder().build()
            val saved = slot<AnimeWithChildren>()
            every { animeService.save(capture(saved)) } answers { stored(firstArg()) }

            // When
            ingestionService.saveAll(listOf(harvest(true, harvestedEpisode(harvestedAnime()))))

            // Then
            assertEquals(existing.anime.id, saved.captured.anime.id)
        }

        @Test
        @DisplayName("should persist every source link but keep the stored anime metadata")
        fun `should persist every source link but keep the stored anime metadata`() {
            // Given: an anime already stored from the highest priority source
            val existing = AnimeWithChildrenMockKBuilder {
                anime = AnimeMockKBuilder { id = Uuid.random() }.build()
                platforms = listOf(
                    AnimePlatformMockKBuilder { platform = Platform.ANIMATION_DIGITAL_NETWORK }.build()
                )
            }.build()
            every { animeService.findAllByNormalizedTitles(any()) } returns mapOf("Title" to existing)
            val savedAnime = slot<AnimeWithChildren>()
            every { animeService.save(capture(savedAnime)) } answers { stored(firstArg()) }
            val savedEpisode = slot<EpisodeWithChildren>()
            every { episodeService.save(capture(savedEpisode)) } returns EpisodeWithChildrenMockKBuilder().build()

            val lowerPriority = harvestedAnime(id = "456", platform = Platform.CRUNCHYROLL)

            // When
            ingestionService.saveAll(listOf(harvest(true, harvestedEpisode(lowerPriority, id = "4001"))))

            // Then: the metadata is kept, but the source link and the episode are persisted
            assertEquals(existing.anime.id, savedAnime.captured.anime.id)
            assertTrue(savedAnime.captured.localizations.isEmpty())
            assertTrue(savedAnime.captured.images.isEmpty())
            assertEquals("456", savedAnime.captured.platforms.single().platformId)
            assertEquals(existing.anime.id, savedEpisode.captured.episode.animeId)
            assertEquals("4001", savedEpisode.captured.platforms.single().platformId)
        }

        @Test
        @DisplayName("should map the harvested children to the stored aggregate")
        fun `should map the harvested children to the stored aggregate`() {
            // Given
            every { animeService.findAllByNormalizedTitles(any()) } returns emptyMap()
            every { episodeService.save(any()) } returns EpisodeWithChildrenMockKBuilder().build()
            val saved = slot<AnimeWithChildren>()
            every { animeService.save(capture(saved)) } answers { stored(firstArg()) }

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
        @DisplayName("should persist the episode of the harvested anime")
        fun `should persist the episode of the harvested anime`() {
            // Given
            every { animeService.findAllByNormalizedTitles(any()) } returns emptyMap()
            every { animeService.save(any()) } answers { stored(firstArg()) }
            val saved = slot<EpisodeWithChildren>()
            every { episodeService.save(capture(saved)) } returns EpisodeWithChildrenMockKBuilder().build()

            // When
            ingestionService.saveAll(listOf(harvest(true, harvestedEpisode(harvestedAnime(), id = "3001"))))

            // Then
            val episode = saved.captured
            assertEquals(persistedAnimeId, episode.episode.animeId)
            assertEquals(1, episode.episode.season)
            assertEquals(1, episode.episode.number)
            assertEquals(EpisodeType.EPISODE, episode.episode.episodeType)
            assertEquals("3001", episode.platforms.single().platformId)
            assertEquals(Country.FR.defaultLocale, episode.localizations.single().locale)
            assertEquals(ImageType.BANNER, episode.images.single().type)
        }

        @Test
        @DisplayName("should persist the platform links and the episodes of every source")
        fun `should persist the platform links and the episodes of every source`() {
            // Given: both platforms harvest the same title
            every { animeService.findAllByNormalizedTitles(any()) } returns emptyMap()
            val savedAnime = slot<AnimeWithChildren>()
            every { animeService.save(capture(savedAnime)) } answers { stored(firstArg()) }
            val savedEpisodes = mutableListOf<EpisodeWithChildren>()
            every { episodeService.save(capture(savedEpisodes)) } returns EpisodeWithChildrenMockKBuilder().build()

            val winning = harvestedEpisode(harvestedAnime(id = "123"), id = "3001")
            val losing = harvestedEpisode(
                harvestedAnime(id = "456", platform = Platform.CRUNCHYROLL),
                id = "4001"
            )

            // When
            ingestionService.saveAll(listOf(harvest(true, winning, losing)))

            // Then: the anime carries both source links and each source keeps its release
            assertEquals(
                setOf("123", "456"),
                savedAnime.captured.platforms.map { it.platformId }.toSet()
            )
            assertEquals(
                listOf("3001", "4001"),
                savedEpisodes.map { it.platforms.single().platformId }
            )
        }

        @Test
        @DisplayName("should keep the stored episode metadata when a lower priority source is harvested")
        fun `should keep the stored episode metadata when a lower priority source is harvested`() {
            // Given: the episode is already stored from the highest priority source
            val existing = AnimeWithChildrenMockKBuilder {
                anime = AnimeMockKBuilder { id = Uuid.random() }.build()
                platforms = listOf(
                    AnimePlatformMockKBuilder { platform = Platform.ANIMATION_DIGITAL_NETWORK }.build()
                )
            }.build()
            every { animeService.findAllByNormalizedTitles(any()) } returns mapOf("Title" to existing)
            every { animeService.save(any()) } answers { stored(firstArg()) }
            val storedEpisode = storedEpisode(Platform.ANIMATION_DIGITAL_NETWORK)
            every {
                episodeService.findByAnimeIdAndSeasonAndNumberAndEpisodeType(any(), any(), any(), any())
            } returns storedEpisode
            val saved = slot<EpisodeWithChildren>()
            every { episodeService.save(capture(saved)) } returns EpisodeWithChildrenMockKBuilder().build()

            val lowerPriority = harvestedAnime(platform = Platform.CRUNCHYROLL)

            // When
            ingestionService.saveAll(listOf(harvest(true, harvestedEpisode(lowerPriority, id = "4001"))))

            // Then: only the release is added, the metadata of the best source is kept
            assertEquals(storedEpisode.episode.id, saved.captured.episode.id)
            assertEquals(1200, saved.captured.episode.duration)
            assertTrue(saved.captured.localizations.isEmpty())
            assertTrue(saved.captured.images.isEmpty())
            assertEquals("4001", saved.captured.platforms.single().platformId)
        }

        @Test
        @DisplayName("should take the episode metadata of a better priority source over the stored one")
        fun `should take the episode metadata of a better priority source over the stored one`() {
            // Given: the episode is stored from a lower priority source
            val existing = AnimeWithChildrenMockKBuilder {
                anime = AnimeMockKBuilder { id = Uuid.random() }.build()
                platforms = listOf(
                    AnimePlatformMockKBuilder { platform = Platform.CRUNCHYROLL }.build()
                )
            }.build()
            every { animeService.findAllByNormalizedTitles(any()) } returns mapOf("Title" to existing)
            every { animeService.save(any()) } answers { stored(firstArg()) }
            val storedEpisode = storedEpisode(Platform.CRUNCHYROLL)
            every {
                episodeService.findByAnimeIdAndSeasonAndNumberAndEpisodeType(any(), any(), any(), any())
            } returns storedEpisode
            val saved = slot<EpisodeWithChildren>()
            every { episodeService.save(capture(saved)) } returns EpisodeWithChildrenMockKBuilder().build()

            // When
            ingestionService.saveAll(listOf(harvest(true, harvestedEpisode(harvestedAnime(), id = "3001"))))

            // Then: the better source owns the metadata, on the stored row
            assertEquals(storedEpisode.episode.id, saved.captured.episode.id)
            assertEquals(1440, saved.captured.episode.duration)
            assertEquals("Episode", saved.captured.localizations.single().title)
            assertEquals("3001", saved.captured.platforms.single().platformId)
        }

        @Test
        @DisplayName("should skip the save workflow when no platform reports a change")
        fun `should skip the save workflow when no platform reports a change`() {
            // Given
            every { animeService.findAllByNormalizedTitles(any()) } returns emptyMap()
            every { animeService.save(any()) } answers { stored(firstArg()) }

            // When
            val saved = ingestionService.saveAll(listOf(harvest(false, harvestedEpisode(harvestedAnime()))))

            // Then: the workflow enters no read and no write
            assertEquals(0, saved)
            verify(exactly = 0) { animeService.findAllByNormalizedTitles(any()) }
            verify(exactly = 0) { animeService.save(any()) }
            verify(exactly = 0) { episodeService.save(any()) }
        }

        @Test
        @DisplayName("should run the save workflow when one platform reports a change")
        fun `should run the save workflow when one platform reports a change`() {
            // Given
            every { animeService.findAllByNormalizedTitles(any()) } returns emptyMap()
            every { animeService.save(any()) } answers { stored(firstArg()) }
            every { episodeService.save(any()) } returns EpisodeWithChildrenMockKBuilder().build()
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
            verify(exactly = 2) { episodeService.save(any()) }
        }

        @Test
        @DisplayName("should run the save workflow on every reported change")
        fun `should run the save workflow on every reported change`() {
            // Given
            every { animeService.findAllByNormalizedTitles(any()) } returns emptyMap()
            every { animeService.save(any()) } answers { stored(firstArg()) }
            every { episodeService.save(any()) } returns EpisodeWithChildrenMockKBuilder().build()

            // When: every platform of the pooled harvest reports a change
            val saved = ingestionService.saveAll(
                listOf(
                    harvest(true, harvestedEpisode(harvestedAnime())),
                    harvest(true, harvestedEpisode(harvestedAnime(id = "456", title = "Other"), id = "1002")),
                    harvest(true, harvestedEpisode(harvestedAnime(id = "789", title = "Third"), id = "1003"))
                )
            )

            // Then: every reported change is honored
            assertEquals(3, saved)
            verify(exactly = 3) { animeService.save(any()) }
            verify(exactly = 3) { episodeService.save(any()) }
        }

        @Test
        @DisplayName("should skip a harvest that reports a change but carries no episode")
        fun `should skip a harvest that reports a change but carries no episode`() {
            // Given
            every { animeService.findAllByNormalizedTitles(any()) } returns emptyMap()
            every { animeService.save(any()) } answers { stored(firstArg()) }

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
            every { animeService.save(any()) } answers { stored(firstArg()) }

            // When
            val saved = ingestionService.saveAll(emptyList())

            // Then: nothing to persist, the persistence is not even read
            assertEquals(0, saved)
            verify(exactly = 0) { animeService.findAllByNormalizedTitles(any()) }
            verify(exactly = 0) { animeService.save(any()) }
        }
    }
}
