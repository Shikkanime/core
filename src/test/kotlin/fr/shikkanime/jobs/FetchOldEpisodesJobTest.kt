package fr.shikkanime.jobs

import com.google.inject.Inject
import fr.shikkanime.caches.CountryCodeIdKeyCache
import fr.shikkanime.entities.Config
import fr.shikkanime.entities.enums.ConfigPropertyKey
import fr.shikkanime.entities.enums.CountryCode
import fr.shikkanime.platforms.CrunchyrollPlatform
import fr.shikkanime.services.*
import fr.shikkanime.utils.Constant
import fr.shikkanime.utils.MapCache
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate

class FetchOldEpisodesJobTest {
    @Inject
    private lateinit var fetchOldEpisodesJob: FetchOldEpisodesJob

    @Inject
    private lateinit var configService: ConfigService

    @Inject
    private lateinit var animeService: AnimeService

    @Inject
    private lateinit var episodeMappingService: EpisodeMappingService

    @Inject
    private lateinit var episodeVariantService: EpisodeVariantService

    @Inject
    private lateinit var crunchyrollPlatform: CrunchyrollPlatform

    @BeforeEach
    fun setUp() {
        Constant.injector.injectMembers(this)
    }

    @AfterEach
    fun tearDown() {
        episodeVariantService.deleteAll()
        episodeMappingService.deleteAll()
        animeService.deleteAll()
        configService.deleteAll()
        MapCache.invalidate(Config::class.java)
        ImageService.clearPool()
    }

    @Test
    fun `check if Atelier Riza is correctly fetch`() {
        val episodes = fetchOldEpisodesJob.crunchyrollEpisodesCache[CountryCodeIdKeyCache(CountryCode.FR, "GEXH3W2Z5")]

        // If episodes contains the episode 1, it means that the job has worked
        assertNotNull(episodes)
        assertTrue(episodes!!.any { it.episodeMetadata!!.seasonNumber == 1 && it.episodeMetadata!!.number == 1 })
    }

    @Test
    fun `fetch Black Clover`() {
        val episodes = fetchOldEpisodesJob.crunchyrollEpisodesCache[CountryCodeIdKeyCache(CountryCode.FR, "GRE50KV36")]

        assertNotNull(episodes)
        assertTrue(episodes!!.any { it.episodeMetadata!!.seasonNumber == 1 && it.episodeMetadata!!.number == 1 && it.episodeMetadata!!.audioLocale == "ja-JP" })
        assertTrue(episodes.any { it.episodeMetadata!!.seasonNumber == 1 && it.episodeMetadata!!.number == 1 && it.episodeMetadata!!.audioLocale == "fr-FR" })
        assertTrue(episodes.any { it.episodeMetadata!!.seasonNumber == 1 && it.episodeMetadata!!.number == 170 && it.episodeMetadata!!.audioLocale == "ja-JP" })
        assertTrue(episodes.any { it.episodeMetadata!!.seasonNumber == 1 && it.episodeMetadata!!.number == 102 && it.episodeMetadata!!.audioLocale == "fr-FR" })
        assertTrue(episodes.size >= 170)
    }

    @Test
    fun getSimulcasts() {
        val dates = LocalDate.of(2023, 4, 10).datesUntil(LocalDate.of(2023, 4, 25)).toList()
        val simulcasts = runBlocking { fetchOldEpisodesJob.getSimulcasts(CountryCode.FR, dates) }

        assertTrue(simulcasts.isNotEmpty())
        assertEquals(3, simulcasts.size)
        assertTrue(simulcasts.contains("winter-2023"))
        assertTrue(simulcasts.contains("spring-2023"))
    }

    @Test
    fun `getSimulcasts for one date`() {
        val dates = listOf(LocalDate.of(2023, 4, 10))
        val simulcasts = runBlocking { fetchOldEpisodesJob.getSimulcasts(CountryCode.FR, dates) }
        println(simulcasts)

        assertTrue(simulcasts.isNotEmpty())
        assertEquals(3, simulcasts.size)
        assertTrue(simulcasts.contains("winter-2023"))
        assertTrue(simulcasts.contains("spring-2023"))
        assertTrue(simulcasts.contains("summer-2023"))
    }

    @Test
    fun `fix issue #503`() {
        val dates = LocalDate.of(2023, 10, 4).datesUntil(LocalDate.of(2023, 10, 14)).toList()
        configService.save(Config(propertyKey = ConfigPropertyKey.LAST_FETCH_OLD_EPISODES.key, propertyValue = "2023-10-14"))
        configService.save(Config(propertyKey = ConfigPropertyKey.FETCH_OLD_EPISODES_RANGE.key, propertyValue = "14"))
        configService.save(Config(propertyKey = ConfigPropertyKey.FETCH_OLD_EPISODES_LIMIT.key, propertyValue = "8"))
        MapCache.invalidate(Config::class.java)
        crunchyrollPlatform.configuration?.availableCountries?.add(CountryCode.FR)
        val simulcasts = runBlocking { fetchOldEpisodesJob.getSimulcasts(CountryCode.FR, dates) }

        assertTrue(simulcasts.isNotEmpty())
        assertEquals(3, simulcasts.size)
        assertTrue(simulcasts.contains("fall-2023"))
        assertTrue(simulcasts.contains("summer-2023"))

        val series = runBlocking { fetchOldEpisodesJob.getSeries(CountryCode.FR, simulcasts) }
        series.forEach { println("${it.id} - ${it.title}") }

        assumeTrue(series.any { it.id == "GXJHM3NJ5" })
        fetchOldEpisodesJob.run()

        val animes = animeService.findAll()
        assertTrue(animes.any { it.name == "CARDFIGHT!! VANGUARD overDress" })
        val anime = animes.first { it.name == "CARDFIGHT!! VANGUARD overDress" }
        val episodes = episodeMappingService.findAllByAnime(anime)
        // If episodes contains the episode 13 season 3, and episode 12 season 3, it means that the job has worked
        assertTrue(episodes.any { it.season == 3 && it.number == 13 })
    }
}