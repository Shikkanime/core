package fr.shikkanime.jobs.impl

import fr.shikkanime.database.services.EpisodeService
import fr.shikkanime.jobs.platforms.PlatformEpisode
import fr.shikkanime.jobs.platforms.StreamingPlatform
import fr.shikkanime.models.Episode
import fr.shikkanime.models.Platform
import io.mockk.coEvery
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDateTime
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.quartz.JobExecutionContext
import java.time.ZonedDateTime
import kotlin.test.assertTrue

@ExtendWith(MockKExtension::class)
@DisplayName("tests for FetchLatestEpisodesJob")
class FetchLatestEpisodesJobTest {

    @MockK private lateinit var streamingPlatform: StreamingPlatform
    @MockK private lateinit var episodeService: EpisodeService
    @MockK private lateinit var context: JobExecutionContext

    private fun job(vararg platforms: StreamingPlatform) =
        FetchLatestEpisodesJob(platforms.toList(), episodeService)

    private fun platformEpisode(id: String, releaseDateTime: ZonedDateTime) =
        PlatformEpisode(
            id = id,
            title = "Episode $id",
            releaseDateTime = releaseDateTime
        )

    @Nested
    @DisplayName("tests for the 'execute' method")
    inner class ExecuteTests {

        @Test
        @DisplayName("should not save episodes released in the future")
        fun `should not save episodes released in the future`() = runTest {
            // Given
            val future = ZonedDateTime.now().plusHours(2)
            coEvery { streamingPlatform.platform } returns Platform.ANIMATION_DIGITAL_NETWORK
            coEvery { streamingPlatform.fetchEpisodes() } returns listOf(platformEpisode("1", future))
            every { episodeService.findAllExistingPlatformEpisodeIds(any(), any()) } returns emptySet()

            // When
            job(streamingPlatform).execute(context)

            // Then
            verify(exactly = 0) { episodeService.save(any(), any(), any(), any()) }
        }

        @Test
        @DisplayName("should not save episodes already present in database")
        fun `should not save episodes already present in database`() = runTest {
            // Given
            val past = ZonedDateTime.now().minusHours(2)
            coEvery { streamingPlatform.platform } returns Platform.ANIMATION_DIGITAL_NETWORK
            coEvery { streamingPlatform.fetchEpisodes() } returns listOf(platformEpisode("2", past))
            every {
                episodeService.findAllExistingPlatformEpisodeIds(
                    Platform.ANIMATION_DIGITAL_NETWORK,
                    listOf("2")
                )
            } returns setOf("2")

            // When
            job(streamingPlatform).execute(context)

            // Then
            verify(exactly = 0) { episodeService.save(any(), any(), any(), any()) }
        }

        @Test
        @DisplayName("should save new released episodes")
        fun `should save new released episodes`() = runTest {
            // Given
            val past = ZonedDateTime.now().minusHours(2)
            val saved = Episode(
                id = kotlin.uuid.Uuid.random(),
                createdAt = LocalDateTime(2026, 1, 10, 8, 0, 0),
                updatedAt = LocalDateTime(2026, 1, 10, 8, 0, 0),
                platform = Platform.ANIMATION_DIGITAL_NETWORK,
                platformEpisodeId = "3",
                title = "Episode 3",
                releaseDateTime = LocalDateTime(2026, 1, 10, 8, 0, 0)
            )
            coEvery { streamingPlatform.platform } returns Platform.ANIMATION_DIGITAL_NETWORK
            coEvery { streamingPlatform.fetchEpisodes() } returns listOf(platformEpisode("3", past))
            every {
                episodeService.findAllExistingPlatformEpisodeIds(
                    Platform.ANIMATION_DIGITAL_NETWORK,
                    listOf("3")
                )
            } returns emptySet()
            every {
                episodeService.save(
                    Platform.ANIMATION_DIGITAL_NETWORK,
                    "3",
                    "Episode 3",
                    any()
                )
            } returns saved

            // When
            job(streamingPlatform).execute(context)

            // Then
            verify(exactly = 1) {
                episodeService.save(Platform.ANIMATION_DIGITAL_NETWORK, "3", "Episode 3", any())
            }
        }

        @Test
        @DisplayName("should continue with other platforms when one platform fails")
        fun `should continue with other platforms when one platform fails`() = runTest {
            // Given
            val otherPlatform = mockkPlatform("other")
            val past = ZonedDateTime.now().minusHours(1)
            coEvery { streamingPlatform.platform } returns Platform.ANIMATION_DIGITAL_NETWORK
            coEvery { streamingPlatform.fetchEpisodes() } throws RuntimeException("boom")
            coEvery { otherPlatform.platform } returns Platform.CRUNCHYROLL
            coEvery { otherPlatform.fetchEpisodes() } returns listOf(platformEpisode("4", past))
            every {
                episodeService.findAllExistingPlatformEpisodeIds(Platform.CRUNCHYROLL, listOf("4"))
            } returns emptySet()
            every {
                episodeService.save(Platform.CRUNCHYROLL, "4", "Episode 4", any())
            } returns mockkSavedEpisode("4")

            // When
            job(streamingPlatform, otherPlatform).execute(context)

            // Then
            verify(exactly = 1) {
                episodeService.save(Platform.CRUNCHYROLL, "4", "Episode 4", any())
            }
        }
    }

    private fun mockkPlatform(name: String): StreamingPlatform =
        io.mockk.mockk<StreamingPlatform>(name = name)

    private fun mockkSavedEpisode(id: String): Episode =
        Episode(
            id = kotlin.uuid.Uuid.random(),
            createdAt = LocalDateTime(2026, 1, 10, 8, 0, 0),
            updatedAt = LocalDateTime(2026, 1, 10, 8, 0, 0),
            platform = Platform.CRUNCHYROLL,
            platformEpisodeId = id,
            title = "Episode $id",
            releaseDateTime = LocalDateTime(2026, 1, 10, 8, 0, 0)
        )
}
