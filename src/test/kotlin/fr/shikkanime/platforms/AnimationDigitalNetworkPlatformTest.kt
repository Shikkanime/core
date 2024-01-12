package fr.shikkanime.platforms

import fr.shikkanime.entities.enums.CountryCode
import fr.shikkanime.platforms.configuration.PlatformSimulcast
import fr.shikkanime.utils.Constant
import jakarta.inject.Inject
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
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
        assertEquals("Helck", episodes[1].anime?.name)
    }

    @Test
    fun `fetchEpisodes for 2024-01-05`() {
        val zonedDateTime = ZonedDateTime.parse("2024-01-05T21:59:59Z")
        val episodes = platform.fetchEpisodes(zonedDateTime)

        assertEquals(true, episodes.isNotEmpty())
        assertEquals(1, episodes.size)
        assertEquals("Pon no Michi", episodes[0].anime?.name)
    }
}