package fr.shikkanime.jobs

import com.google.inject.Inject
import fr.shikkanime.AbstractTest
import fr.shikkanime.entities.Config
import fr.shikkanime.entities.enums.ConfigPropertyKey
import fr.shikkanime.entities.enums.CountryCode
import fr.shikkanime.platforms.CrunchyrollPlatform
import fr.shikkanime.utils.MapCache
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class FetchOldEpisodesJobTest : AbstractTest() {
    @Inject
    private lateinit var fetchOldEpisodesJob: FetchOldEpisodesJob

    @Inject
    private lateinit var crunchyrollPlatform: CrunchyrollPlatform

    @Test
    fun `fix issue #503`() {
        configService.save(Config(propertyKey = ConfigPropertyKey.LAST_FETCH_OLD_EPISODES.key, propertyValue = "2023-10-14"))
        configService.save(Config(propertyKey = ConfigPropertyKey.FETCH_OLD_EPISODES_RANGE.key, propertyValue = "14"))
        configService.save(Config(propertyKey = ConfigPropertyKey.FETCH_OLD_EPISODES_LIMIT.key, propertyValue = "8"))
        MapCache.invalidate(Config::class.java)
        crunchyrollPlatform.configuration?.availableCountries?.add(CountryCode.FR)
        fetchOldEpisodesJob.run()
        val animes = animeService.findAll()
        assertTrue(animes.any { it.name == "CARDFIGHT!! VANGUARD overDress" })
        val anime = animes.first { it.name == "CARDFIGHT!! VANGUARD overDress" }
        val episodes = episodeMappingService.findAllByAnime(anime)
        // If episodes contains the episode 13 season 3, and episode 12 season 3, it means that the job has worked
        assertTrue(episodes.any { it.season == 3 && it.number == 13 })
    }

    @Test
    fun `check crunchyroll calendar with long request`() {
        configService.save(Config(propertyKey = ConfigPropertyKey.LAST_FETCH_OLD_EPISODES.key, propertyValue = "2022-03-07"))
        configService.save(Config(propertyKey = ConfigPropertyKey.FETCH_OLD_EPISODES_RANGE.key, propertyValue = "35"))
        configService.save(Config(propertyKey = ConfigPropertyKey.FETCH_OLD_EPISODES_LIMIT.key, propertyValue = "-1"))
        MapCache.invalidate(Config::class.java)
        crunchyrollPlatform.configuration?.availableCountries?.add(CountryCode.FR)
        fetchOldEpisodesJob.run()
        val animes = animeService.findAll()
        animes.forEach { println(it.name) }

        assertTrue(animes.any { it.name == "Vivy -Fluorite Eye's Song-" })
        assertTrue(animes.any { it.name == "Arifureta: From Commonplace to World's Strongest" })
        assertTrue(animes.any { it.name == "Mushoku Tensei: Jobless Reincarnation" })
        assertTrue(animes.any { it.name == "Our Last Crusade or the Rise of a New World" })

        val anime = animes.first { it.name == "Vivy -Fluorite Eye's Song-" }
        val episodes = episodeMappingService.findAllByAnime(anime)
        assertTrue(episodes.any { it.season == 1 && it.number == 13 })
    }

    @Test
    fun `failed to fetch due to long description`() {
        configService.save(Config(propertyKey = ConfigPropertyKey.LAST_FETCH_OLD_EPISODES.key, propertyValue = "2023-02-13"))
        configService.save(Config(propertyKey = ConfigPropertyKey.FETCH_OLD_EPISODES_RANGE.key, propertyValue = "14"))
        configService.save(Config(propertyKey = ConfigPropertyKey.FETCH_OLD_EPISODES_LIMIT.key, propertyValue = "6"))
        MapCache.invalidate(Config::class.java)
        crunchyrollPlatform.configuration?.availableCountries?.add(CountryCode.FR)
        fetchOldEpisodesJob.run()
        val animes = animeService.findAll()
        animes.forEach { println(it.name) }
    }
}