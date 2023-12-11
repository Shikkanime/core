package fr.shikkanime.platforms

import fr.shikkanime.entities.enums.CountryCode
import fr.shikkanime.utils.Constant
import jakarta.inject.Inject
import java.time.ZonedDateTime
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
    }

    @Test
    fun fetchEpisodes() {
        val zonedDateTime = ZonedDateTime.parse("2023-12-05T21:59:59Z")
        val episodes = platform.fetchEpisodes(zonedDateTime)

        println(episodes)

        assert(episodes.isNotEmpty())
        expect(2) { episodes.size }

        expect("Paradox Live THE ANIMATION") { episodes[0].anime?.name }
        expect("Helck") { episodes[1].anime?.name }
    }
}