package fr.shikkanime.services

import fr.shikkanime.AbstractTest
import fr.shikkanime.entities.Anime
import fr.shikkanime.entities.enums.CountryCode
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class AnimeSearchTest : AbstractTest() {
    @Test
    fun testExactMatchFirst() {
        animeService.save(
            Anime(
                countryCode = CountryCode.FR,
                name = "Bakemonogatari",
                slug = "bakemonogatari"
            )
        )
        animeService.save(
            Anime(
                countryCode = CountryCode.FR,
                name = "Monogatari",
                slug = "monogatari"
            )
        )
        val anime3 = animeService.save(
            Anime(
                countryCode = CountryCode.FR,
                name = "Mono",
                slug = "mono"
            )
        )

        animeService.preIndex()

        val results = animeService.findAllBy(
            countryCode = CountryCode.FR,
            simulcastUuid = null,
            name = "mono",
            searchTypes = null,
            sort = emptyList(),
            page = 1,
            limit = 10
        )

        assertEquals(3, results.data.size)
        // L'animé dont le nom est exactement "mono" (anime3) doit être le premier
        assertEquals(anime3.uuid, results.data.first().uuid)
    }

    @Test
    fun testPartialMatchSecond() {
        animeService.save(
            Anime(
                countryCode = CountryCode.FR,
                name = "Bakemonogatari",
                slug = "bakemonogatari"
            )
        )
        val anime2 = animeService.save(
            Anime(
                countryCode = CountryCode.FR,
                name = "Monogatari",
                slug = "monogatari"
            )
        )

        animeService.preIndex()

        val results = animeService.findAllBy(
            countryCode = CountryCode.FR,
            simulcastUuid = null,
            name = "monogatari",
            searchTypes = null,
            sort = emptyList(),
            page = 1,
            limit = 10
        )

        assertEquals(2, results.data.size)
        // L'animé dont le nom est exactement "Monogatari" (anime2) doit être le premier
        assertEquals(anime2.uuid, results.data.first().uuid)
    }
}
