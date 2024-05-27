package fr.shikkanime.jobs

import com.google.inject.Inject
import fr.shikkanime.caches.CountryCodeIdKeyCache
import fr.shikkanime.entities.Config
import fr.shikkanime.entities.enums.ConfigPropertyKey
import fr.shikkanime.entities.enums.CountryCode
import fr.shikkanime.services.ConfigService
import fr.shikkanime.utils.Constant
import fr.shikkanime.utils.MapCache
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate

class FetchOldEpisodesJobTest {
    @Inject
    private lateinit var fetchOldEpisodesJob: FetchOldEpisodesJob

    @Inject
    private lateinit var configService: ConfigService

    @BeforeEach
    fun setUp() {
        Constant.injector.injectMembers(this)
    }

    @AfterEach
    fun tearDown() {
        configService.deleteAll()
        MapCache.invalidate(Config::class.java)
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

    @Test
    fun getSimulcasts() {
        val dates = LocalDate.of(2023, 4, 10).datesUntil(LocalDate.of(2023, 4, 25)).toList()
        configService.save(Config(propertyKey = ConfigPropertyKey.SIMULCAST_RANGE.key, propertyValue = "20"))
        MapCache.invalidate(Config::class.java)
        val simulcasts = fetchOldEpisodesJob.getSimulcasts(dates)

        assertTrue(simulcasts.isNotEmpty())
        assertEquals(2, simulcasts.size)
        assertEquals("winter-2023", simulcasts.first())
        assertEquals("spring-2023", simulcasts.last())
    }
}