package fr.shikkanime.platforms

import fr.shikkanime.caches.CountryCodePrimeVideoSimulcastKeyCache
import fr.shikkanime.entities.enums.CountryCode
import fr.shikkanime.platforms.configuration.PrimeVideoConfiguration
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.ZonedDateTime

class PrimeVideoPlatformTest {
    private val primeVideoPlatform = PrimeVideoPlatform()

    @Test
    fun fetchApiContent() {
        val key = CountryCodePrimeVideoSimulcastKeyCache(
            CountryCode.FR,
            PrimeVideoConfiguration.PrimeVideoSimulcast(
                1,
                "image",
                "17:00"
            ).apply {
                name = "0QN9ZXJ935YBTNK8U9FV5OAX5B"
            }
        )

        val episodes = runBlocking { primeVideoPlatform.fetchApiContent(key, ZonedDateTime.now()) }
        assertEquals(true, episodes.isNotEmpty())
    }
}