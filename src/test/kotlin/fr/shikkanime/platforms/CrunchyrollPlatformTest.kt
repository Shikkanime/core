package fr.shikkanime.platforms

import fr.shikkanime.entities.Config
import fr.shikkanime.entities.enums.CountryCode
import fr.shikkanime.entities.enums.LangType
import fr.shikkanime.services.ConfigService
import fr.shikkanime.utils.Constant
import fr.shikkanime.utils.MapCache
import jakarta.inject.Inject
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.File
import java.time.ZonedDateTime

class CrunchyrollPlatformTest {
    @Inject
    lateinit var platform: CrunchyrollPlatform

    @Inject
    lateinit var configService: ConfigService

    @BeforeEach
    fun setUp() {
        Constant.injector.injectMembers(this)

        platform.loadConfiguration()
        platform.configuration!!.availableCountries.add(CountryCode.FR)
    }

    @AfterEach
    fun tearDown() {
        configService.deleteAll()
        MapCache.invalidate(Config::class.java)
    }

    @Test
    fun fetchEpisodesJSON() {
        val s = "2024-01-24T18:45:00Z"
        val zonedDateTime = ZonedDateTime.parse(s)

        platform.simulcasts[CountryCode.FR] = setOf("metallic rouge")

        val episodes = platform.fetchEpisodes(
            zonedDateTime,
            File(
                ClassLoader.getSystemClassLoader().getResource("crunchyroll/api-${s.replace(':', '-')}.json")?.file
                    ?: throw Exception("File not found")
            )
        )

        assertEquals(true, episodes.isNotEmpty())
        assertEquals(2, episodes.size)
        assertEquals("Metallic Rouge", episodes[0].anime?.name)
        assertEquals(LangType.VOICE, episodes[0].langType)
        assertNotNull(episodes[0].description)
        assertEquals("Metallic Rouge", episodes[1].anime?.name)
        assertEquals(LangType.SUBTITLES, episodes[1].langType)
        assertNotNull(episodes[1].description)
    }
}