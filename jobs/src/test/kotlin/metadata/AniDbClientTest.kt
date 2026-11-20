package fr.shikkanime.jobs.metadata

import fr.shikkanime.database.services.ApiCacheService
import fr.shikkanime.models.MetadataPlatform
import io.mockk.mockk
import java.time.LocalDate
import org.jsoup.Jsoup
import org.jsoup.parser.Parser
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AniDbClientTest {
    private val cache = MetadataApiCache(mockk<ApiCacheService>(relaxed = true))
    private val client = AniDbClient(cache)

    private fun fixture(name: String): String =
        requireNotNull(javaClass.getResourceAsStream("/metadata/$name"))
            .bufferedReader().readText()

    @Test
    fun `parses a full anime document`() {
        // Given: the real AniDB document of aid 1552 (Sgt. Frog / Keroro Gunsou)
        val xml = fixture("anidb_anime_1552.xml")

        // When
        val anime = client.parseAnimeDocument(xml, 1552)

        // Then
        assertNotNull(anime)
        assertEquals("1552", anime!!.id)
        assertEquals(MetadataPlatform.ANIDB, anime.platform)
        assertTrue(anime.titles.contains("Sgt. Frog"))
        assertTrue(anime.titles.contains("Sergeant Keroro"))
        assertEquals("516", anime.externalIds[MetadataPlatform.MYANIMELIST])
        assertEquals(2004, anime.year)
        assertTrue(anime.episodes.size > 300)
        assertTrue(anime.relatedIds.contains("20085"))
    }

    @Test
    fun `reads the episode number from epno type 1`() {
        // Given
        val xml = fixture("anidb_anime_11061.xml")

        // When
        val anime = client.parseAnimeDocument(xml, 11061)

        // Then
        assertNotNull(anime)
        assertEquals(2, anime!!.episodes.size)
        assertEquals(setOf(1, 2), anime.episodes.map { it.number }.toSet())
        assertEquals(LocalDate.of(2015, 4, 17), anime.episodes.first().airDate)
        assertEquals("165774", anime.episodes.first().id)
        assertEquals("29915", anime.externalIds[MetadataPlatform.MYANIMELIST])
    }

    @Test
    fun `returns null on an AniDB error document`() {
        // Given
        val xml = """<error code="260">login required</error>"""

        // Then
        assertNull(client.parseAnimeDocument(xml, 1))
    }

    @Test
    fun `ranks title index matches by similarity`() {
        // Given: a minimal title index shaped like the real anime-titles.xml
        val xml = """
            <animetitles>
              <anime aid="1"><title>One Piece</title><title>ワンピース</title></anime>
              <anime aid="1552"><title>Sgt. Frog</title><title>Keroro Gunsou</title>
                <title>Sergeant Keroro</title></anime>
              <anime aid="999"><title>Attack on Titan</title><title>Shingeki no Kyojin</title></anime>
            </animetitles>
        """.trimIndent()

        // When
        val results = client.searchTitleIndex(xml, "Sergent Keroro")

        // Then
        assertEquals("1552", results.first().id)
        assertTrue(results.none { it.id == "999" })
    }
}
