package fr.shikkanime.services.caches

import com.google.inject.Inject
import fr.shikkanime.AbstractTest
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class BotDetectorCacheTest : AbstractTest() {
    @Inject private lateinit var botDetectorCache: BotDetectorCache

    @Test
    fun testBotDetectorCache() {
        assertTrue(botDetectorCache.isBot(clientIp = "69.171.251.9"))
        assertFalse(botDetectorCache.isBot(clientIp = "234.60.195.104"))
        assertTrue(botDetectorCache.isBot(userAgent = "Mozilla/5.0 (compatible; Bytespider; spider-feedback@bytedance.com) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/70.0.0.0 Safari/537.36"))
        assertTrue(botDetectorCache.isBot(userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/129.0.0.0 Safari/537.36 (StatusCake)"))
        assertTrue(botDetectorCache.isBot(userAgent = "Mozilla/5.0 (Linux; Android 6.0.1; Nexus 5X Build/MMB29P) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0.6723.69 Mobile Safari/537.36 (compatible; Googlebot/2.1; +http://www.google.com/bot.html)"))
        assertFalse(botDetectorCache.isBot(userAgent = "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0.0.0 Safari/537.36"))
    }
}