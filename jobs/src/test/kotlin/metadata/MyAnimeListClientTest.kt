package fr.shikkanime.jobs.metadata

import fr.shikkanime.database.services.ApiCacheService
import fr.shikkanime.models.MetadataPlatform
import io.mockk.mockk
import java.time.LocalDate
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MyAnimeListClientTest {
    private val cache = MetadataApiCache(mockk<ApiCacheService>(relaxed = true))
    private val client = MyAnimeListClient(cache)

    private fun fixture(name: String): String =
        requireNotNull(javaClass.getResourceAsStream("/metadata/$name"))
            .bufferedReader().readText()

    @Test
    fun `maps the full entry to an external anime`() {
        // Given: the real tenrai /full response for MAL 516 (Sgt. Frog)
        val media = cache.decode<MyAnimeListMediaResponse>(fixture("mal_anime_516.json")).data!!

        // When
        val anime = client.run { media.toExternalAnime() }

        // Then
        assertEquals("516", anime.id)
        assertEquals(MetadataPlatform.MYANIMELIST, anime.platform)
        assertEquals(setOf("1552"), anime.externalIds[MetadataPlatform.ANIDB]?.let(::setOf))
        assertTrue(anime.titles.contains("Keroro Gunsou"))
        assertTrue(anime.titles.contains("Sgt. Frog"))
        assertTrue(anime.titles.contains("ケロロ軍曹"))
        assertNotNull(anime.description)
        assertEquals(358, anime.episodeCount)
        assertEquals(2004, anime.year)
    }

    @Test
    fun `maps search results to external animes`() {
        // Given: the real tenrai search response for "Sergent Keroro"
        val response = cache.decode<MyAnimeListSearchResponse>(fixture("mal_search_keroro.json"))

        // When
        val animes = response.data.map { client.run { it.toExternalAnime() } }

        // Then: Sgt. Frog is among the matches; franchise siblings share its titles, so the
        // linker - not the raw search - settles which entry is the right one
        assertTrue(animes.any { it.id == "516" && it.titles.contains("Sgt. Frog") })
    }

    @Test
    fun `numbers episodes by rank inside the external anime`() {
        // Given: the real first page of episodes of MAL 516
        val response = cache.decode<MyAnimeListEpisodeResponse>(
            fixture("mal_episodes_516_p1.json")
        )

        // When: same mapping as getEpisodesByMediaId
        val episodes = response.data.mapIndexed { index, episode ->
            ExternalEpisode(
                id = "516/${episode.id}",
                number = index + 1,
                title = episode.title ?: episode.titleRomanji ?: episode.titleJapanese,
                airDate = episode.aired?.take(10)?.let(LocalDate::parse)
            )
        }

        // Then
        assertEquals(100, episodes.size)
        assertEquals("516/1", episodes.first().id)
        assertEquals(1, episodes.first().number)
        assertEquals(LocalDate.of(2004, 4, 3), episodes.first().airDate)
        assertEquals(100, episodes.last().number)
    }
}
