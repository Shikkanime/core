package fr.shikkanime.jobs

import com.google.inject.Inject
import fr.shikkanime.caches.CountryCodeIdKeyCache
import fr.shikkanime.entities.enums.CountryCode
import fr.shikkanime.utils.Constant
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class FetchOldEpisodesJobTest {
    @Inject
    private lateinit var fetchOldEpisodesJob: FetchOldEpisodesJob

    @BeforeEach
    fun setUp() {
        Constant.injector.injectMembers(this)
    }

    @Test
    fun `check if Atelier Riza is correctly fetch`() {
        val episodes = fetchOldEpisodesJob.crunchyrollEpisodesCache[CountryCodeIdKeyCache(CountryCode.FR, "GEXH3W2Z5")]

        // If episodes contains the episode 1, it means that the job has worked
        assertNotNull(episodes)
        assertTrue(episodes!!.any { it.seasonNumber == 1 && it.number == 1 })
    }

    @Test
    fun `fetch Black Clover`() {
        val episodes = fetchOldEpisodesJob.crunchyrollEpisodesCache[CountryCodeIdKeyCache(CountryCode.FR, "GRE50KV36")]

        assertNotNull(episodes)
        assertTrue(episodes!!.any { it.seasonNumber == 1 && it.number == 1 })
        assertTrue(episodes.any { it.seasonNumber == 1 && it.number == 170 })
        assertTrue(episodes.size >= 170)
    }
}