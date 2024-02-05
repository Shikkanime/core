package fr.shikkanime.platforms

import fr.shikkanime.entities.enums.CountryCode
import fr.shikkanime.entities.enums.LangType
import fr.shikkanime.platforms.configuration.PlatformSimulcast
import fr.shikkanime.utils.Constant
import jakarta.inject.Inject
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.ZonedDateTime
import java.util.*

class AnimationDigitalNetworkPlatformTest {
    @Inject
    lateinit var platform: AnimationDigitalNetworkPlatform

    @BeforeEach
    fun setUp() {
        Constant.injector.injectMembers(this)

        platform.loadConfiguration()
        platform.configuration!!.availableCountries.add(CountryCode.FR)
        platform.configuration!!.simulcasts.add(PlatformSimulcast(UUID.randomUUID(), "Pon no Michi"))
    }

    @AfterEach
    fun tearDown() {
        platform.configuration!!.availableCountries.remove(CountryCode.FR)
        platform.configuration!!.simulcasts.removeIf { it.name == "Pon no Michi" }
        platform.reset()
    }

    @Test
    fun `fetchEpisodes for 2023-12-05`() {
        val zonedDateTime = ZonedDateTime.parse("2023-12-05T21:59:59Z")
        val episodes = platform.fetchEpisodes(zonedDateTime)

        assertEquals(true, episodes.isNotEmpty())
        assertEquals(2, episodes.size)
        assertEquals("Paradox Live THE ANIMATION", episodes[0].anime?.name)
        assertNotNull(episodes[0].description)
        assertEquals("Helck", episodes[1].anime?.name)
        assertNotNull(episodes[1].description)
    }

    @Test
    fun `fetchEpisodes for 2024-01-05`() {
        val zonedDateTime = ZonedDateTime.parse("2024-01-05T21:59:59Z")
        val episodes = platform.fetchEpisodes(zonedDateTime)

        assertEquals(true, episodes.isNotEmpty())
        assertEquals(1, episodes.size)
        assertEquals("Pon no Michi", episodes[0].anime?.name)
        assertNotNull(episodes[0].description)
    }

    @Test
    fun `fetchEpisodes for 2024-01-21`() {
        val zonedDateTime = ZonedDateTime.parse("2024-01-21T21:59:59Z")
        val episodes = platform.fetchEpisodes(zonedDateTime)

        assertEquals(true, episodes.isNotEmpty())
        assertEquals(4, episodes.size)
        assertEquals("One Piece", episodes[0].anime?.name)
        assertNotNull(episodes[0].description)
        assertEquals("Run For Money", episodes[1].anime?.name)
        assertNotNull(episodes[1].description)

        assertEquals("MONSTERS", episodes[2].anime?.name)
        assertEquals(LangType.SUBTITLES, episodes[2].langType)
        assertNotNull(episodes[2].description)
        assertEquals(LangType.VOICE, episodes[3].langType)
        assertNotNull(episodes[3].description)
    }

    @Test
    fun `fetchEpisodes for 2022-12-09`() {
        val zonedDateTime = ZonedDateTime.parse("2022-12-09T23:59:59Z")
        val episodes = platform.fetchEpisodes(zonedDateTime)

        assertEquals(true, episodes.isNotEmpty())
        assertEquals(2, episodes.size)
        assertEquals("Les Héros de la Galaxie : Die Neue These", episodes[0].anime?.name)
        assertNotNull(episodes[0].description)
        assertEquals("My Master Has No Tail", episodes[1].anime?.name)
        assertNotNull(episodes[1].description)
    }

    @Test
    fun `fetchEpisodes for 2022-03-18`() {
        val zonedDateTime = ZonedDateTime.parse("2022-03-18T23:59:59Z")
        val episodes = platform.fetchEpisodes(zonedDateTime)

        assertEquals(true, episodes.isNotEmpty())
        assertEquals(1, episodes.size)
        assertEquals("Les Héros de la Galaxie : Die Neue These", episodes[0].anime?.name)
        assertNotNull(episodes[0].description)
    }

    @Test
    fun `fetchEpisodes for 2024-02-01`() {
        val zonedDateTime = ZonedDateTime.parse("2024-02-01T23:59:59Z")
        val episodes = platform.fetchEpisodes(zonedDateTime)

        assertEquals(true, episodes.isNotEmpty())
        assertEquals(3, episodes.size)
        assertEquals("Demon Slave", episodes[0].anime?.name)
        assertNotNull(episodes[0].description)
        assertEquals("My Instant Death Ability Is So Overpowered, No One in This Other World Stands a Chance Against Me!", episodes[1].anime?.name)
        assertNotNull(episodes[1].description)
        assertEquals("Urusei Yatsura", episodes[2].anime?.name)
        assertNotNull(episodes[2].description)
    }
}