package fr.shikkanime.services

import com.google.inject.Inject
import fr.shikkanime.entities.*
import fr.shikkanime.entities.enums.ConfigPropertyKey
import fr.shikkanime.entities.enums.CountryCode
import fr.shikkanime.entities.enums.EpisodeType
import fr.shikkanime.utils.Constant
import fr.shikkanime.utils.MapCache
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import java.time.ZonedDateTime

class EpisodeVariantServiceTest {
    @Inject
    private lateinit var episodeVariantService: EpisodeVariantService

    @Inject
    private lateinit var configService: ConfigService

    @Inject
    private lateinit var animeService: AnimeService

    @BeforeEach
    fun setUp() {
        Constant.injector.injectMembers(this)
        configService.save(Config(propertyKey = ConfigPropertyKey.SIMULCAST_RANGE.key, propertyValue = "15"))
        MapCache.invalidate(Config::class.java, Anime::class.java)
    }

    @AfterEach
    fun tearDown() {
        configService.deleteAll()
        animeService.deleteAll()
        MapCache.invalidate(Config::class.java, Anime::class.java)
    }

    @Test
    fun `getSimulcasts get next`() {
        val anime = Anime(countryCode = CountryCode.FR, name = "Test Anime", image = "test.jpg", slug = "test-anime")
        val episode = mock(EpisodeMapping::class.java)

        anime.releaseDateTime = ZonedDateTime.parse("2023-12-20T16:00:00Z")
        `when`(episode.releaseDateTime).thenReturn(anime.releaseDateTime)

        animeService.save(anime)
        val simulcast = episodeVariantService.getSimulcast(anime, episode)

        assertEquals("WINTER", simulcast.season)
        assertEquals(2024, simulcast.year)
    }

    @Test
    fun `getSimulcasts continue on current`() {
        val anime = Anime(countryCode = CountryCode.FR, name = "Test Anime", image = "test.jpg", slug = "test-anime")
        val episode = mock(EpisodeMapping::class.java)
        val finalRelease = ZonedDateTime.parse("2024-01-03T16:00:00Z")

        anime.releaseDateTime = finalRelease.minusWeeks(12)
        anime.simulcasts.add(Simulcast(season = "AUTUMN", year = 2023))

        anime.mappings.addAll((1..<12).map { i ->
            EpisodeMapping(
                anime = anime,
                episodeType = EpisodeType.EPISODE,
                season = 1,
                number = i,
                releaseDateTime = anime.releaseDateTime.plusWeeks(i.toLong()),
                variants = mutableSetOf(EpisodeVariant(audioLocale = "ja-JP"))
            )
        })

        `when`(episode.releaseDateTime).thenReturn(finalRelease)
        `when`(episode.episodeType).thenReturn(EpisodeType.EPISODE)
        `when`(episode.number).thenReturn(12)

        animeService.save(anime)
        val simulcast = episodeVariantService.getSimulcast(anime, episode)

        assertEquals("AUTUMN", simulcast.season)
        assertEquals(2023, simulcast.year)
    }
}