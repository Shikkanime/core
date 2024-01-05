package fr.shikkanime.platforms

import fr.shikkanime.entities.enums.CountryCode
import fr.shikkanime.platforms.configuration.PlatformSimulcast
import fr.shikkanime.utils.Constant
import jakarta.inject.Inject
import java.time.ZonedDateTime
import java.util.*
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.expect

class AnimationDigitalNetworkPlatformTest {
    @Inject
    lateinit var platform: AnimationDigitalNetworkPlatform

    @BeforeTest
    fun setUp() {
        Constant.injector.injectMembers(this)

        platform.loadConfiguration()
        platform.configuration!!.availableCountries.add(CountryCode.FR)
        platform.configuration!!.simulcasts.add(PlatformSimulcast(UUID.randomUUID(), "Pon no Michi"))
    }

    @AfterTest
    fun tearDown() {
        platform.configuration!!.availableCountries.remove(CountryCode.FR)
        platform.configuration!!.simulcasts.removeIf { it.name == "Pon no Michi" }
        platform.reset()
    }

    @Test
    fun `fetchEpisodes for 2023-12-05`() {
        val zonedDateTime = ZonedDateTime.parse("2023-12-05T21:59:59Z")
        val episodes = platform.fetchEpisodes(zonedDateTime)

        println(episodes)

        assert(episodes.isNotEmpty())
        expect(2) { episodes.size }

        expect("Paradox Live THE ANIMATION") { episodes[0].anime?.name }
        expect("Helck") { episodes[1].anime?.name }
    }

    @Test
    fun `fetchEpisodes for 2024-01-05`() {
        val zonedDateTime = ZonedDateTime.parse("2024-01-05T21:59:59Z")
        val episodes = platform.fetchEpisodes(zonedDateTime)

        println(episodes)

        assert(episodes.isNotEmpty())
        expect(1) { episodes.size }

        expect("Pon no Michi") { episodes[0].anime?.name }
    }
}