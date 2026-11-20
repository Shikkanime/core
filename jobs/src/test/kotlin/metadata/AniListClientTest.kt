package fr.shikkanime.jobs.metadata

import fr.shikkanime.database.services.ApiCacheService
import fr.shikkanime.models.MetadataPlatform
import io.mockk.mockk
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class AniListClientTest {
    private val cache = MetadataApiCache(mockk<ApiCacheService>(relaxed = true))
    private val client = AniListClient(cache)

    private fun fixture(name: String): String =
        requireNotNull(javaClass.getResourceAsStream("/metadata/$name"))
            .bufferedReader().readText()

    @Test
    fun `maps a search result to an external anime`() {
        // Given: the real AniList search response for "Sgt. Frog"
        val response = cache.decode<AniListPageResponse>(
            fixture("anilist_search_sergeant_keroro.json")
        )
        val media = response.data!!.page!!.media!!.first()

        // When
        val anime = client.run { media.toExternalAnime() }

        // Then
        assertEquals("516", anime.id)
        assertEquals(MetadataPlatform.ANILIST, anime.platform)
        assertEquals("516", anime.externalIds[MetadataPlatform.MYANIMELIST])
        assertTrue(anime.titles.contains("Keroro Gunsou"))
        assertTrue(anime.titles.contains("Sgt. Frog"))
        assertTrue(anime.titles.contains("ケロロ軍曹"))
        assertTrue(anime.titles.contains("Sargento Keroro"))
        assertNotNull(anime.description)
        assertTrue(anime.episodeCount!! > 0)
        assertEquals(2004, anime.year)
    }

    @Test
    fun `keeps only anime relations as related ids`() {
        // Given
        val response = cache.decode<AniListPageResponse>(
            fixture("anilist_search_sergeant_keroro.json")
        )
        val media = response.data!!.page!!.media!!.first()

        // When
        val anime = client.run { media.toExternalAnime() }

        // Then: every related id is numeric and the set is not empty
        assertTrue(anime.relatedIds.isNotEmpty())
        assertTrue(anime.relatedIds.all { it.all(Char::isDigit) })
    }
}
