package fr.shikkanime.wrappers

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class BskyWrapperTest {
    @Test
    fun getFacets() {
        val facets1 = BskyWrapper.getFacets("\uD83C\uDF7F 3 nouveaux épisodes sont disponibles !\n" +
                "\n" +
                "• L'épisode 2 de A Gentle Noble's Vacation Recommendation\n" +
                "• L'épisode 3 de Jack-of-All-Trades, Party of None\n" +
                "• L'épisode 1 de Oshi no Ko\n" +
                "\n" +
                "#Anime")

        println(facets1)
        assertEquals(1, facets1.second.size)
        assertEquals(193, facets1.second.first().start)
        assertEquals(199, facets1.second.first().end)

        val facets2 = BskyWrapper.getFacets("\uD83C\uDF7F 2 nouveaux épisodes sont disponibles !\n" +
                "\n" +
                "• L'épisode 2 de An Adventurer's Daily Grind at Age 29\n" +
                "• L'épisode 2 de Easygoing Territory Defense by the Optimistic Lord\n" +
                "\n" +
                "#Anime")

        println(facets2)
        assertEquals(1, facets2.second.size)
        assertEquals(176, facets2.second.first().start)
        assertEquals(182, facets2.second.first().end)

        val facets3 = BskyWrapper.getFacets("\uD83D\uDCFA L'épisode 2 de #YoroiShindenSamuraiTroopers est maintenant disponible en VOSTFR !\n" +
                "\n" +
                "#Anime")

        println(facets3)
        assertEquals(2, facets3.second.size)
        assertEquals(21, facets3.second.first().start)
        assertEquals(49, facets3.second.first().end)
        assertEquals(89, facets3.second.last().start)
        assertEquals(95, facets3.second.last().end)
    }
}