package fr.shikkanime.wrappers

import fr.shikkanime.entities.enums.CountryCode
import fr.shikkanime.wrappers.factories.AbstractCrunchyrollWrapper
import fr.shikkanime.wrappers.impl.caches.CrunchyrollCachedWrapper
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.Test
import java.time.LocalDate
import kotlin.test.assertTrue

class CrunchyrollWrapperTest {
    private val locale = CountryCode.FR.locale

    @Test
    fun getBrowse() {
        val newlyAdded = runBlocking { CrunchyrollCachedWrapper.getBrowse(locale) }
        assertNotNull(newlyAdded)
        assertTrue(newlyAdded.size in (24..25))
    }

    @Test
    fun getWinter2024Series() {
        val series = runBlocking {
            CrunchyrollCachedWrapper.getBrowse(
                locale,
                sortBy = AbstractCrunchyrollWrapper.SortType.POPULARITY,
                type = AbstractCrunchyrollWrapper.MediaType.SERIES,
                size = 200,
                simulcast = "winter-2024"
            )
        }
        assertEquals(true, series.isNotEmpty())

        series.forEach {
            println(it.title)
        }
    }

    @Test
    fun getPreviousEpisode() {
        val previousEpisode = runBlocking { CrunchyrollCachedWrapper.getPreviousEpisode(locale, "G14U47QGQ") }
        assertNotNull(previousEpisode)
        assertEquals("G4VUQ5K25", previousEpisode.id)
    }

    @Test
    fun getUpNext() {
        val nextEpisode = runBlocking { CrunchyrollCachedWrapper.getUpNext(locale, "G14U47QGQ") }
        assertNotNull(nextEpisode)
        assertEquals("GJWU2WNE7", nextEpisode.id)
    }

    @Test
    fun getSimulcastCalendarWithDates() {
        val episodes = CrunchyrollCachedWrapper.getSimulcastCalendarWithDates(
            CountryCode.FR,
            setOf(LocalDate.parse("2024-01-01"))
        )

        assumeTrue(episodes.isNotEmpty())
    }

    @Test
    fun testSpamSeriesRequest() {
        assertNotNull(runBlocking { CrunchyrollCachedWrapper.getSeries(locale, "GREXH8DQ2") })
        assertNotNull(runBlocking { CrunchyrollCachedWrapper.getSeries(locale, "G8DHV722J") })
        assertNotNull(runBlocking { CrunchyrollCachedWrapper.getSeries(locale, "G9VHN9P99") })
        assertNotNull(runBlocking { CrunchyrollCachedWrapper.getSeries(locale, "GYZJEEQGR") })
        assertNotNull(runBlocking { CrunchyrollCachedWrapper.getSeries(locale, "GKEH2G0N1") })
        assertNotNull(runBlocking { CrunchyrollCachedWrapper.getSeries(locale, "G0XHWM1MK") })
        assertNotNull(runBlocking { CrunchyrollCachedWrapper.getSeries(locale, "GEXH3W2W7") })
        assertNotNull(runBlocking { CrunchyrollCachedWrapper.getSeries(locale, "GQWH0M9N8") })
        assertNotNull(runBlocking { CrunchyrollCachedWrapper.getSeries(locale, "GZJH3DXJG") })
        assertNotNull(runBlocking { CrunchyrollCachedWrapper.getSeries(locale, "G9VHN91DJ") })
        assertNotNull(runBlocking { CrunchyrollCachedWrapper.getSeries(locale, "G79H23Z3P") })
    }
}