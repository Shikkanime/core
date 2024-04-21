package fr.shikkanime.services

import com.google.inject.Inject
import fr.shikkanime.converters.AbstractConverter
import fr.shikkanime.dtos.EpisodeMappingDto
import fr.shikkanime.entities.Anime
import fr.shikkanime.entities.EpisodeMapping
import fr.shikkanime.entities.EpisodeVariant
import fr.shikkanime.entities.enums.CountryCode
import fr.shikkanime.entities.enums.EpisodeType
import fr.shikkanime.utils.Constant
import fr.shikkanime.utils.MapCache
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class EpisodeMappingServiceTest {
    @Inject
    private lateinit var animeService: AnimeService

    @Inject
    private lateinit var episodeMappingService: EpisodeMappingService

    @BeforeEach
    fun setUp() {
        Constant.injector.injectMembers(this)
    }

    @AfterEach
    fun tearDown() {
        episodeMappingService.deleteAll()
        animeService.deleteAll()
        MapCache.invalidate(EpisodeMapping::class.java, EpisodeVariant::class.java)
    }

    @Test
    fun update() {
        val anime = animeService.save(Anime(countryCode = CountryCode.FR, name = "Test Anime", image = "test.jpg", slug = "test-anime"))
        val episodeMapping = episodeMappingService.save(
            EpisodeMapping(
                anime = anime,
                episodeType = EpisodeType.SPECIAL,
                season = 1,
                number = 1,
                image = "test.jpg",
            )
        )

        val dto = AbstractConverter.convert(episodeMapping, EpisodeMappingDto::class.java)
        dto.episodeType = EpisodeType.FILM

        val updated = episodeMappingService.update(episodeMapping.uuid!!, dto)
        assertEquals(EpisodeType.FILM, updated!!.episodeType)
    }

    @Test
    fun `update with different identifier`() {
        val anime = animeService.save(
            Anime(
                countryCode = CountryCode.FR,
                name = "Test Anime",
                image = "test.jpg",
                slug = "test-anime"
            )
        )
        episodeMappingService.save(
            EpisodeMapping(
                anime = anime,
                episodeType = EpisodeType.FILM,
                season = 1,
                number = 1,
                image = "test.jpg",
            )
        )

        val episodeMapping = episodeMappingService.save(
            EpisodeMapping(
                anime = anime,
                episodeType = EpisodeType.SPECIAL,
                season = 1,
                number = 1,
                image = "test.jpg",
            )
        )

        val dto = AbstractConverter.convert(episodeMapping, EpisodeMappingDto::class.java)
        dto.episodeType = EpisodeType.FILM

        val updated = episodeMappingService.update(episodeMapping.uuid!!, dto)
        assertNotEquals(updated!!.uuid, episodeMapping.uuid)
        assertEquals(EpisodeType.FILM, updated.episodeType)
        assertNull(episodeMappingService.find(episodeMapping.uuid!!))
    }
}