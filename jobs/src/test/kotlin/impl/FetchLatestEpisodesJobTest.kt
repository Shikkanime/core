package fr.shikkanime.jobs.impl

import fr.shikkanime.jobs.diagnostics.IngestionRun
import fr.shikkanime.jobs.diagnostics.IngestionRunRegistry
import fr.shikkanime.jobs.platforms.StreamingPlatform
import fr.shikkanime.models.Platform
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDateTime
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.quartz.JobExecutionContext
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@DisplayName("tests for the fetch latest episodes job")
class FetchLatestEpisodesJobTest {

    private val context = mockk<JobExecutionContext>(relaxed = true)

    private val fetchedAt = LocalDateTime(2026, 1, 10, 8, 0, 0)

    private fun runFor(platform: Platform) = IngestionRun(
        platform = platform,
        verdicts = emptyList(),
        fetchedAt = fetchedAt
    )

    private fun platformReturning(platform: Platform, run: IngestionRun): StreamingPlatform =
        mockk<StreamingPlatform>().also {
            every { it.platform } returns platform
            coEvery { it.diagnoseLatestEpisodes() } returns run
        }

    @Test
    @DisplayName("should record the run of every platform")
    fun `should record the run of every platform`() = runTest {
        // Given
        val adnRun = runFor(Platform.ANIMATION_DIGITAL_NETWORK)
        val crunRun = runFor(Platform.CRUNCHYROLL)
        val registry = IngestionRunRegistry()
        val job = FetchLatestEpisodesJob(
            platforms = listOf(
                platformReturning(Platform.ANIMATION_DIGITAL_NETWORK, adnRun),
                platformReturning(Platform.CRUNCHYROLL, crunRun)
            ),
            registry = registry
        )

        // When
        job.execute(context)

        // Then
        assertEquals(adnRun, registry.latestRun(Platform.ANIMATION_DIGITAL_NETWORK))
        assertEquals(crunRun, registry.latestRun(Platform.CRUNCHYROLL))
        assertEquals(2, registry.latestRuns().size)
    }

    @Test
    @DisplayName("should keep diagnosing the other platforms when one throws")
    fun `should keep diagnosing the other platforms when one throws`() = runTest {
        // Given
        val healthyRun = runFor(Platform.CRUNCHYROLL)
        val broken = mockk<StreamingPlatform>().also {
            every { it.platform } returns Platform.ANIMATION_DIGITAL_NETWORK
            coEvery { it.diagnoseLatestEpisodes() } throws RuntimeException("HTTP 500")
        }
        val registry = IngestionRunRegistry()
        val job = FetchLatestEpisodesJob(
            platforms = listOf(broken, platformReturning(Platform.CRUNCHYROLL, healthyRun)),
            registry = registry
        )

        // When
        job.execute(context)

        // Then a broken platform must not hide the working ones
        assertEquals(healthyRun, registry.latestRun(Platform.CRUNCHYROLL))
        assertTrue(
            registry.latestRun(Platform.ANIMATION_DIGITAL_NETWORK)?.error?.contains("HTTP 500") == true,
            "the failure must be recorded, got: ${registry.latestRun(Platform.ANIMATION_DIGITAL_NETWORK)}"
        )
    }

    @Test
    @DisplayName("should replace a previous successful run when the platform starts failing")
    fun `should replace a previous successful run when the platform starts failing`() = runTest {
        // Given a registry already holding a healthy run from an earlier tick
        val healthyRun = runFor(Platform.ANIMATION_DIGITAL_NETWORK)
        val registry = IngestionRunRegistry()
        registry.record(healthyRun)
        val platform = mockk<StreamingPlatform>().also {
            every { it.platform } returns Platform.ANIMATION_DIGITAL_NETWORK
            coEvery { it.diagnoseLatestEpisodes() } throws RuntimeException("HTTP 500")
        }

        // When
        FetchLatestEpisodesJob(platforms = listOf(platform), registry = registry).execute(context)

        // Then the stale healthy run must not survive, otherwise it looks like a fresh result
        val latest = registry.latestRun(Platform.ANIMATION_DIGITAL_NETWORK)
        assertTrue(latest !== healthyRun, "the stale run must be replaced")
        assertTrue(latest?.error != null, "the replacement must carry the failure")
    }
}
