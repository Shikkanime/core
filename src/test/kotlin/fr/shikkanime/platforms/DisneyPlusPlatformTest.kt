package fr.shikkanime.platforms

import com.google.inject.Inject
import fr.shikkanime.AbstractTest
import fr.shikkanime.entities.Anime
import fr.shikkanime.entities.EpisodeMapping
import fr.shikkanime.entities.EpisodeVariant
import fr.shikkanime.entities.enums.CountryCode
import fr.shikkanime.entities.enums.EpisodeType
import fr.shikkanime.entities.enums.Platform
import fr.shikkanime.jobs.FetchEpisodesJob
import fr.shikkanime.platforms.configuration.DisneyPlusConfiguration
import fr.shikkanime.services.caches.EpisodeVariantCacheService
import fr.shikkanime.utils.MapCache
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.File
import java.time.ZonedDateTime

class DisneyPlusPlatformTest : AbstractTest() {
    @Inject
    private lateinit var episodeVariantCacheService: EpisodeVariantCacheService

    @Inject
    private lateinit var fetchEpisodesJob: FetchEpisodesJob

    @Inject
    private lateinit var platform: DisneyPlusPlatform

    @BeforeEach
    override fun setUp() {
        super.setUp()
        platform.loadConfiguration()
        platform.configuration!!.availableCountries = setOf(CountryCode.FR)
    }

    @AfterEach
    override fun tearDown() {
        super.tearDown()
        platform.configuration!!.simulcasts.clear()
    }

    @Test
    fun fetchEpisodesJSON() {
        val entityId = "cd0d005f-85d3-4d8c-bfc7-a46be3f9a2ce"
        val s = "2024-12-11T18:45:00Z"
        val zonedDateTime = ZonedDateTime.parse(s)

        platform.configuration!!.simulcasts.add(
            DisneyPlusConfiguration.DisneyPlusSimulcast(
                releaseDay = zonedDateTime.dayOfWeek.value,
                releaseTime = "00:00:00"
            ).apply {
                name = entityId
            }
        )

        val anime = animeService.save(
            Anime(
                countryCode = CountryCode.FR,
                name = "Murai in Love",
                slug = "murai-in-love",
                releaseDateTime = zonedDateTime,
                image = "test",
                banner = "test",
            )
        )

        val mapping = episodeMappingService.save(
            EpisodeMapping(
                anime = anime,
                releaseDateTime = zonedDateTime,
                episodeType = EpisodeType.EPISODE,
                season = 1,
                number = 1,
                duration = 1440,
                image = "test",
            )
        )

        episodeVariantService.save(
            EpisodeVariant(
                mapping = mapping,
                releaseDateTime = zonedDateTime,
                platform = Platform.DISN,
                audioLocale = "ja-JP",
                identifier = "FR-DISN-e106f116-496f-4503-bfe0-b003b28eb45f-JA-JP",
                url = "test",
            )
        )

        MapCache.invalidate(Anime::class.java, EpisodeMapping::class.java, EpisodeVariant::class.java)
        val variants = episodeVariantCacheService.findAll()
        fetchEpisodesJob.addHashCaches(platform, variants)

        val episodes = platform.fetchEpisodes(
            zonedDateTime,
            File(
                ClassLoader.getSystemClassLoader().getResource("disney_plus/$entityId/anime.json")?.file
                    ?: throw Exception("File not found")
            )
        )

        assertEquals(true, episodes.isNotEmpty())
        assertEquals(11, episodes.size)
    }
}