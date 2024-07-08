package fr.shikkanime.services

import com.google.inject.Inject
import fr.shikkanime.entities.*
import fr.shikkanime.entities.enums.ConfigPropertyKey
import fr.shikkanime.entities.enums.CountryCode
import fr.shikkanime.entities.enums.EpisodeType
import fr.shikkanime.entities.enums.Platform
import fr.shikkanime.platforms.AbstractPlatform
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
    private lateinit var episodeMappingService: EpisodeMappingService

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
        episodeVariantService.deleteAll()
        episodeMappingService.deleteAll()
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
        animeService.save(anime)

        (1..<12).map { i ->
            episodeMappingService.save(
                EpisodeMapping(
                anime = anime,
                episodeType = EpisodeType.EPISODE,
                season = 1,
                number = i,
                releaseDateTime = anime.releaseDateTime.plusWeeks(i.toLong()),
                    image = "test.jpg",
                )
            ).apply {
                episodeVariantService.save(
                    EpisodeVariant(
                        mapping = this,
                        platform = Platform.CRUN,
                        audioLocale = "ja-JP",
                        identifier = "test-episode-$i",
                        url = "https://test.com/episode-$i",
                    )
                )
            }
        }

        `when`(episode.releaseDateTime).thenReturn(finalRelease)
        `when`(episode.episodeType).thenReturn(EpisodeType.EPISODE)
        `when`(episode.number).thenReturn(12)

        val simulcast = episodeVariantService.getSimulcast(anime, episode)

        assertEquals("AUTUMN", simulcast.season)
        assertEquals(2023, simulcast.year)
    }

    @Test
    fun `save platform episode`() {
        episodeVariantService.save(
            AbstractPlatform.Episode(
                CountryCode.FR,
                "Shikimori n’est pas juste mignonne",
                "https://www.crunchyroll.com/imgsrv/display/thumbnail/1560x2340/catalog/crunchyroll/57da95e93614672250ff0312b4c8194c.jpe",
                "https://www.crunchyroll.com/imgsrv/display/thumbnail/1920x1080/catalog/crunchyroll/c1aa33105d2acdcf7807310743b01948.jpe",
                "Izumi est un lycéen maladroit et malchanceux. Pourtant, c’est ce qui fait son charme et lui a permis de sortir avec Shikimori. Cette camarade de classe est très jolie, elle a un beau sourire et semble toujours heureuse en compagnie d’Izumi. Pourtant, le garçon ne peut s’empêcher de complexer ! Il fait tout pour continuer de la séduire, même si ses actions ne l’aident pas vraiment dans sa tâche…",
                ZonedDateTime.parse("2021-05-21T18:15:00Z"),
                EpisodeType.SPECIAL,
                1,
                -1,
                1404,
                "Commentaire audio",
                "Les interprètes de Shikimori, d’Izumi et de Hachimitsu commentent le premier épisode.",
                "https://www.crunchyroll.com/imgsrv/display/thumbnail/1920x1080/catalog/crunchyroll/4dfb75a0af21c5ca84014d47f67ad176.jpe",
                Platform.CRUN,
                "ja-JP",
                "GVWU0Q0J9",
                "https://www.crunchyroll.com/fr/watch/GVWU0Q0J9/special",
                false
            ),
            updateMappingDateTime = false
        )

        val mappings = episodeMappingService.findAll()
        assertEquals(1, mappings.size)
        val mapping = mappings.first()
        assertEquals(EpisodeType.SPECIAL, mapping.episodeType)
        assertEquals(1, mapping.season)
        assertEquals(1, mapping.number)
    }
}