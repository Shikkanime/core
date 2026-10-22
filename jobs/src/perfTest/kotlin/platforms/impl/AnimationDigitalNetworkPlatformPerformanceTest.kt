package fr.shikkanime.jobs.platforms.impl

import fr.shikkanime.jobs.SmartHttpClient
import fr.shikkanime.jobs.perf.BasePlatformPerformanceTest
import fr.shikkanime.models.Country
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respondOk
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.time.ZonedDateTime

@DisplayName("tests for AnimationDigitalNetworkPlatform performance")
class AnimationDigitalNetworkPlatformPerformanceTest : BasePlatformPerformanceTest() {

    private val sampleDate: ZonedDateTime = ZonedDateTime.parse("2026-09-21T12:00:00Z")

    /**
     * Instantiates a mock [SmartHttpClient] with a mock engine for standalone platform testing.
     *
     * @return A configured [SmartHttpClient] instance.
     * @see [SmartHttpClient]
     * @see [AnimationDigitalNetworkPlatform]
     */
    private fun createMockClient(): SmartHttpClient {
        val mockEngine = MockEngine { respondOk("{}") }
        return SmartHttpClient(client = HttpClient(mockEngine))
    }

    @Test
    @DisplayName("should convert one million episodes of a realistic catalog within budget")
    fun shouldConvertOneMillionEpisodesOfARealisticCatalogWithinBudget() {
        // Given
        val shows = Array(1000) { i ->
            val isLiveAction = (i % 10 == 0)
            AdnShow(
                id = i + 1,
                title = "Anime Title ${i + 1}",
                shortTitle = null,
                originalTitle = null,
                summary = "Anime description ${i + 1}\nLine 2",
                image2x = "https://image.animationdigitalnetwork.fr/" +
                    "show/${i + 1}/100x100/portrait-with-logo",
                imageHorizontal2x = "https://image.animationdigitalnetwork.fr/" +
                    "show/${i + 1}/100x100/landscape-with-logo",
                type = AdnType.EPS,
                genres = if (isLiveAction)
                    listOf("Live Action", "Drama")
                else
                    listOf("Animation japonaise", "Action")
            )
        }

        val videos = ArrayList<AdnVideo>(1_000_000)
        for (i in 1..1_000_000) {
            val show = shows[i % 1000]
            val videoType = when {
                i % 50 == 0 -> AdnType.PV
                i % 100 == 0 -> AdnType.BONUS
                else -> AdnType.EPS
            }
            val shortNumber = when {
                i % 40 == 0 -> "Bande-annonce 1"
                i % 80 == 0 -> "Opening 1"
                else -> "${i % 1000 + 1}"
            }

            videos.add(
                AdnVideo(
                    id = i,
                    name = "Episode $i",
                    title = null,
                    shortNumber = shortNumber,
                    season = null,
                    type = videoType,
                    image2x = "https://image.animationdigitalnetwork.fr/video/$i/100x100/eps",
                    summary = "Summary of episode $i",
                    releaseDate = sampleDate,
                    duration = 1430L,
                    url = "https://animationdigitalnetwork.fr/video/$i",
                    languages = listOf("vostf"),
                    show = show
                )
            )
        }
        val platform = AnimationDigitalNetworkPlatform(createMockClient())

        // When & Then
        measureAndAssert(
            label = "benchmark-adn-realistic-catalog",
            itemCount = 1_000_000,
            expectedOutputSize = 900_000
        ) {
            platform.convertVideos(videos, Country.FR)
        }
    }
}
