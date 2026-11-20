package fr.shikkanime.database.services

import fr.shikkanime.core.pagination.PageRequest
import fr.shikkanime.database.repositories.EpisodeGroupRepository
import fr.shikkanime.database.services.impl.EpisodeGroupServiceImpl
import fr.shikkanime.models.Country
import fr.shikkanime.models.EpisodeObservation
import fr.shikkanime.models.EpisodeType
import fr.shikkanime.models.Locale
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.verify
import kotlinx.datetime.LocalDateTime
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.extension.ExtendWith
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.uuid.Uuid

@ExtendWith(MockKExtension::class)
@DisplayName("tests for the episode group service")
class EpisodeGroupServiceTest {
    @MockK private lateinit var groupRepository: EpisodeGroupRepository

    private lateinit var service: EpisodeGroupServiceImpl

    @BeforeEach
    fun setUp() {
        service = EpisodeGroupServiceImpl(groupRepository)
    }

    private fun observation(
        animeId: Uuid = Uuid.random(),
        episodeId: Uuid = Uuid.random(),
        season: Int = 1,
        number: Int = 1,
        releaseId: Uuid = Uuid.random(),
        audioLocale: Locale = Locale.JA_JP,
        releaseDateTime: LocalDateTime = LocalDateTime.parse("2026-06-01T10:00:00")
    ) = EpisodeObservation(
        animeId = animeId,
        animeSlug = "test-anime",
        episodeId = episodeId,
        season = season,
        episodeType = EpisodeType.EPISODE,
        number = number,
        releaseId = releaseId,
        audioLocale = audioLocale,
        releaseDateTime = releaseDateTime
    )

    private fun stubObservations(observations: List<EpisodeObservation>) {
        every {
            groupRepository.findObservations(any(), any(), any(), any())
        } returns observations
        every {
            groupRepository.findAudioLocalesByEpisodeIds(any())
        } answers {
            firstArg<List<Uuid>>().associateWith { episodeId ->
                observations.filter { it.episodeId == episodeId }
                    .map { it.audioLocale }
                    .toSet()
            }
        }
        every { groupRepository.findReleasesByIds(any()) } returns emptyMap()
    }

    @Nested
    @DisplayName("tests for the 'findLatest' method")
    inner class FindLatestTests {
        @Test
        @DisplayName("should drop releases whose locale is hidden by a CJK sibling")
        fun `should drop releases whose locale is hidden by a CJK sibling`() {
            // Given: one episode released in ja-JP and en-US — en is not a
            // fallback here because Japanese subtitles exist
            val episodeId = Uuid.random()
            stubObservations(
                listOf(
                    observation(episodeId = episodeId, audioLocale = Locale.JA_JP),
                    observation(episodeId = episodeId, audioLocale = Locale.EN_US)
                )
            )

            // When
            val page = service.findLatest(
                country = Country.FR,
                from = LocalDateTime.parse("2026-01-01T00:00:00"),
                to = null,
                animeId = null,
                episodeType = null,
                searchTypes = null,
                request = PageRequest(1, 20)
            )

            // Then: only the Japanese release survives grouping
            val releases = page.data.single().group.releases
            assertEquals(1, releases.size)
            assertEquals(Locale.JA_JP, releases.single().audioLocale)
        }

        @Test
        @DisplayName("should keep English when it is the only subtitle locale")
        fun `should keep English when it is the only subtitle locale`() {
            // Given
            stubObservations(listOf(observation(audioLocale = Locale.EN_US)))

            // When
            val page = service.findLatest(
                country = Country.FR,
                from = LocalDateTime.parse("2026-01-01T00:00:00"),
                to = null,
                animeId = null,
                episodeType = null,
                searchTypes = null,
                request = PageRequest(1, 20)
            )

            // Then
            assertEquals(1, page.data.single().group.releases.size)
        }

        @Test
        @DisplayName("should serve the newest groups first and paginate the computed set")
        fun `should serve the newest groups first and paginate the computed set`() {
            // Given: three groups on distinct days — same anime, different slots
            val animeId = Uuid.random()
            stubObservations(
                listOf(
                    observation(animeId = animeId, number = 1,
                        releaseDateTime = LocalDateTime.parse("2026-06-01T10:00:00")),
                    observation(animeId = animeId, number = 2,
                        releaseDateTime = LocalDateTime.parse("2026-06-10T10:00:00")),
                    observation(animeId = animeId, number = 3,
                        releaseDateTime = LocalDateTime.parse("2026-06-20T10:00:00"))
                )
            )

            // When
            val page = service.findLatest(
                country = Country.FR,
                from = LocalDateTime.parse("2026-01-01T00:00:00"),
                to = null,
                animeId = null,
                episodeType = null,
                searchTypes = null,
                request = PageRequest(1, 2)
            )

            // Then: newest first, page bounds over the whole computed set
            assertEquals(2, page.data.size)
            assertEquals(3L, page.totalElements)
            assertEquals(
                LocalDateTime.parse("2026-06-20T10:00:00"),
                page.data[0].group.releaseDateTime
            )
            assertEquals(
                LocalDateTime.parse("2026-06-10T10:00:00"),
                page.data[1].group.releaseDateTime
            )
        }

        @Test
        @DisplayName("should hydrate releases of the requested page only")
        fun `should hydrate releases of the requested page only`() {
            // Given: two groups, page size one — only the newest group's release is fetched
            val newestReleaseId = Uuid.random()
            stubObservations(
                listOf(
                    observation(number = 1,
                        releaseDateTime = LocalDateTime.parse("2026-06-01T10:00:00")),
                    observation(number = 2, releaseId = newestReleaseId,
                        releaseDateTime = LocalDateTime.parse("2026-06-20T10:00:00"))
                )
            )

            // When
            service.findLatest(
                country = Country.FR,
                from = LocalDateTime.parse("2026-01-01T00:00:00"),
                to = null,
                animeId = null,
                episodeType = null,
                searchTypes = null,
                request = PageRequest(1, 1)
            )

            // Then
            verify {
                groupRepository.findReleasesByIds(match { ids -> newestReleaseId in ids })
            }
        }
    }
}
