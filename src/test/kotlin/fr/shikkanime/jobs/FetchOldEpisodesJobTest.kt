package fr.shikkanime.jobs

import com.google.inject.Inject
import fr.shikkanime.entities.Config
import fr.shikkanime.entities.enums.ConfigPropertyKey
import fr.shikkanime.services.*
import fr.shikkanime.utils.Constant
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class FetchOldEpisodesJobTest {
    @Inject
    private lateinit var fetchOldEpisodesJob: FetchOldEpisodesJob

    @Inject
    private lateinit var configService: ConfigService

    @Inject
    private lateinit var animeService: AnimeService

    @Inject
    private lateinit var episodeVariantService: EpisodeVariantService

    @Inject
    private lateinit var episodeMappingService: EpisodeMappingService

    @Inject
    private lateinit var simulcastService: SimulcastService

    @BeforeEach
    fun setUp() {
        Constant.injector.injectMembers(this)
    }

    @AfterEach
    fun tearDown() {
        configService.deleteAll()
        episodeVariantService.deleteAll()
        episodeMappingService.deleteAll()
        animeService.deleteAll()
        simulcastService.deleteAll()
    }

    @Test
    fun run() {
        configService.save(
            Config(
                propertyKey = ConfigPropertyKey.LAST_FETCH_OLD_EPISODES.key,
                propertyValue = "2024-01-10"
            )
        )
        configService.save(Config(propertyKey = ConfigPropertyKey.FETCH_OLD_EPISODES_RANGE.key, propertyValue = "14"))
        fetchOldEpisodesJob.run()

        val animes = animeService.findAll()
        assertTrue(animes.isNotEmpty())
        assertTrue(animes.all { episodeMappingService.findAllByAnime(it).isNotEmpty() })
        assertTrue(animes.all { episodeVariantService.findAllByAnime(it).isNotEmpty() })
    }
}