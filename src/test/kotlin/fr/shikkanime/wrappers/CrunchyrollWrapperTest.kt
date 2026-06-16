package fr.shikkanime.wrappers

import fr.shikkanime.entities.enums.CountryCode
import fr.shikkanime.wrappers.factories.AbstractCrunchyrollWrapper
import fr.shikkanime.wrappers.impl.caches.CrunchyrollCachedWrapper
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.Test

class CrunchyrollWrapperTest {
    private val locale = CountryCode.FR.locale

    @Test
    suspend fun getBrowse() {
        val newlyAdded = CrunchyrollCachedWrapper.getBrowse(locale)
        assertNotNull(newlyAdded)
        assumeTrue(25 == newlyAdded.size)
    }

    @Test
    suspend fun getWinter2024Series() {
        val series = CrunchyrollCachedWrapper.getBrowse(
            locale,
            sortBy = AbstractCrunchyrollWrapper.SortType.POPULARITY,
            type = AbstractCrunchyrollWrapper.MediaType.SERIES,
            size = 100,
            simulcast = "winter-2024"
        )
        assertEquals(true, series.isNotEmpty())

        series.forEach {
            println(it.title)
        }
    }

    @Test
    suspend fun getPreviousEpisode() {
        val previousEpisode = CrunchyrollCachedWrapper.getPreviousEpisode(locale, "G14U47QGQ")
        assertNotNull(previousEpisode)
        assertEquals("G4VUQ5K25", previousEpisode.id)
    }

    @Test
    suspend fun getUpNext() {
        val nextEpisode = CrunchyrollCachedWrapper.getUpNext(locale, "G14U47QGQ")
        assertNotNull(nextEpisode)
        assertEquals("GJWU2WNE7", nextEpisode.id)
    }

    @Test
    suspend fun testSpamSeriesRequest() {
        assertNotNull(CrunchyrollCachedWrapper.getSeries(locale, "GREXH8DQ2"))
        assertNotNull(CrunchyrollCachedWrapper.getSeries(locale, "G8DHV722J"))
        assertNotNull(CrunchyrollCachedWrapper.getSeries(locale, "G9VHN9P99"))
        assertNotNull(CrunchyrollCachedWrapper.getSeries(locale, "GYZJEEQGR"))
        assertNotNull(CrunchyrollCachedWrapper.getSeries(locale, "GKEH2G0N1"))
        assertNotNull(CrunchyrollCachedWrapper.getSeries(locale, "G0XHWM1MK"))
        assertNotNull(CrunchyrollCachedWrapper.getSeries(locale, "GEXH3W2W7"))
        assertNotNull(CrunchyrollCachedWrapper.getSeries(locale, "GQWH0M9N8"))
        assertNotNull(CrunchyrollCachedWrapper.getSeries(locale, "GZJH3DXJG"))
        assertNotNull(CrunchyrollCachedWrapper.getSeries(locale, "G9VHN91DJ"))
        assertNotNull(CrunchyrollCachedWrapper.getSeries(locale, "G79H23Z3P"))
    }
}