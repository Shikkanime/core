package fr.shikkanime.jobs

import com.google.inject.Inject
import fr.shikkanime.entities.Anime
import fr.shikkanime.entities.EpisodeMapping
import fr.shikkanime.entities.EpisodeVariant
import fr.shikkanime.entities.enums.CountryCode
import fr.shikkanime.entities.enums.EpisodeType
import fr.shikkanime.entities.enums.Platform
import fr.shikkanime.services.AnimeService
import fr.shikkanime.services.EpisodeMappingService
import fr.shikkanime.services.EpisodeVariantService
import fr.shikkanime.utils.Constant
import fr.shikkanime.utils.MapCache
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.ZonedDateTime

class UpdateEpisodeJobTest {
    @Inject
    private lateinit var updateEpisodeJob: UpdateEpisodeJob

    @Inject
    private lateinit var animeService: AnimeService

    @Inject
    private lateinit var episodeMappingService: EpisodeMappingService

    @Inject
    private lateinit var episodeVariantService: EpisodeVariantService

    @BeforeEach
    fun setUp() {
        Constant.injector.injectMembers(this)
    }

    @AfterEach
    fun tearDown() {
        episodeVariantService.deleteAll()
        episodeMappingService.deleteAll()
        animeService.deleteAll()
        MapCache.invalidate(Anime::class.java, EpisodeMapping::class.java, EpisodeVariant::class.java)
    }

    @Test
    fun `run old Crunchyroll episodes`() {
        val zonedDateTime = ZonedDateTime.now().minusMonths(2)
        val anime = animeService.save(
            Anime(
                countryCode = CountryCode.FR,
                releaseDateTime = zonedDateTime,
                lastReleaseDateTime = zonedDateTime,
                name = "Rent-a-Girlfriend",
                slug = "rent-a-girlfriend",
                image = "test.jpg"
            )
        )
        val episodeMapping = episodeMappingService.save(
            EpisodeMapping(
                anime = anime,
                releaseDateTime = zonedDateTime,
                lastReleaseDateTime = zonedDateTime,
                lastUpdateDateTime = zonedDateTime,
                season = 1,
                episodeType = EpisodeType.EPISODE,
                number = 1,
                image = "test.jpg"
            )
        )
        episodeVariantService.save(
            EpisodeVariant(
                mapping = episodeMapping,
                releaseDateTime = zonedDateTime,
                platform = Platform.CRUN,
                audioLocale = "fr-FR",
                identifier = "FR-CRUN-GZ7UV8KWZ-FR-FR",
                url = "https://www.crunchyroll.com/fr/watch/GZ7UV8KWZ/rent-a-girlfriend"
            )
        )
        updateEpisodeJob.run()

        val animes = animeService.findAll()
        assertEquals(1, animes.size)
        val mappings = episodeMappingService.findAll()
        assertEquals(1, mappings.size)
        val variants = episodeVariantService.findAll()
        assertEquals(2, variants.size)
    }

    @Test
    fun `run old ADN episodes`() {
        val zonedDateTime = ZonedDateTime.now().minusMonths(2)
        val anime = animeService.save(
            Anime(
                countryCode = CountryCode.FR,
                releaseDateTime = zonedDateTime,
                lastReleaseDateTime = zonedDateTime,
                name = "The Eminence in Shadow",
                slug = "the-eminence-in-shadow",
                image = "test.jpg"
            )
        )
        val episodeMapping = episodeMappingService.save(
            EpisodeMapping(
                anime = anime,
                releaseDateTime = zonedDateTime,
                lastReleaseDateTime = zonedDateTime,
                lastUpdateDateTime = zonedDateTime,
                season = 1,
                episodeType = EpisodeType.EPISODE,
                number = 1,
                image = "test.jpg"
            )
        )
        episodeVariantService.save(
            EpisodeVariant(
                mapping = episodeMapping,
                releaseDateTime = zonedDateTime,
                platform = Platform.ANIM,
                audioLocale = "ja-JP",
                identifier = "FR-ANIM-20568-JA-JP",
                url = "https://animationdigitalnetwork.fr/video/the-eminence-in-shadow/20568-episode-1-un-camarade-detestable"
            )
        )
        updateEpisodeJob.run()

        val animes = animeService.findAll()
        assertEquals(1, animes.size)
        val mappings = episodeMappingService.findAll()
        assertEquals(1, mappings.size)
        val variants = episodeVariantService.findAll()
        assertEquals(2, variants.size)
    }
}