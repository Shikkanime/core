package fr.shikkanime.wrappers

import fr.shikkanime.entities.enums.CountryCode
import fr.shikkanime.wrappers.impl.caches.AnimationDigitalNetworkCachedWrapper
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class AnimationDigitalNetworkWrapperTest {
    @Test
    fun getPreviousEpisode() {
        val previousEpisode = runBlocking { AnimationDigitalNetworkCachedWrapper.getPreviousVideo(CountryCode.FR, 1160, 26664) }
        assertNotNull(previousEpisode)
        assertEquals(26663, previousEpisode?.id)
    }

    @Test
    fun getUpNext() {
        val nextEpisode = runBlocking { AnimationDigitalNetworkCachedWrapper.getNextVideo(CountryCode.FR, 1160, 26664) }
        assertNotNull(nextEpisode)
        assertEquals(26665, nextEpisode?.id)
    }

    @Test
    fun `getPreviousEpisode #2`() {
        val previousEpisode = runBlocking { AnimationDigitalNetworkCachedWrapper.getPreviousVideo(CountryCode.FR, 565, 10114) }
        assertNotNull(previousEpisode)
        assertEquals(10113, previousEpisode?.id)
    }

    @Test
    fun `getUpNext #2`() {
        val nextEpisode = runBlocking { AnimationDigitalNetworkCachedWrapper.getNextVideo(CountryCode.FR, 565, 10114) }
        assertNull(nextEpisode)
    }
}