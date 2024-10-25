package fr.shikkanime.platforms

import com.google.inject.Inject
import fr.shikkanime.caches.CountryCodePrimeVideoSimulcastKeyCache
import fr.shikkanime.entities.enums.CountryCode
import fr.shikkanime.platforms.configuration.PrimeVideoConfiguration
import fr.shikkanime.utils.Constant
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.ZonedDateTime

class PrimeVideoPlatformTest {
    @Inject
    private lateinit var primeVideoPlatform: PrimeVideoPlatform

    @BeforeEach
    fun setUp() {
        Constant.injector.injectMembers(this)
    }

    @Test
    fun fetchApiContent() {
        val countryCode = CountryCode.FR
        val zonedDateTime = ZonedDateTime.parse("2024-04-22T18:15:00Z")
        val key = CountryCodePrimeVideoSimulcastKeyCache(countryCode, PrimeVideoConfiguration.PrimeVideoSimulcast(
            1,
            "https://cdn.myanimelist.net/images/anime/1142/141351.jpg",
            "16:01:00"
        ).apply {
            name = "0QN9ZXJ935YBTNK8U9FV5OAX5B"
        })
        val episodes = runBlocking { primeVideoPlatform.fetchApiContent(key, zonedDateTime) }

        assumeTrue(episodes.isNotEmpty())
        assumeTrue(episodes.size == 13)

        episodes.forEach {
            assertTrue(it.image.startsWith("https://m.media-amazon.com"))
            assertTrue(it.url.isNotBlank())
            assertFalse(it.getIdentifier().contains("https://"))
        }
    }
}