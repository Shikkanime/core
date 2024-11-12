package fr.shikkanime.wrappers

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import kotlin.test.assertNull

class AnimationDigitalNetworkWrapperTest {
    @Test
    fun getPreviousEpisode() {
        val previousEpisode = runBlocking { AnimationDigitalNetworkWrapper.getPreviousVideo(26664, 1160) }
        assertNotNull(previousEpisode)
        assertEquals(26663, previousEpisode?.id)
    }

    @Test
    fun getUpNext() {
        val nextEpisode = runBlocking { AnimationDigitalNetworkWrapper.getNextVideo(26664, 1160) }
        assertNotNull(nextEpisode)
        assertEquals(26665, nextEpisode?.id)
    }

    @Test
    fun `getPreviousEpisode #2`() {
        val previousEpisode = runBlocking { AnimationDigitalNetworkWrapper.getPreviousVideo(10114, 565) }
        assertNotNull(previousEpisode)
        assertEquals(10113, previousEpisode?.id)
    }

    @Test
    fun `getUpNext #2`() {
        val nextEpisode = runBlocking { AnimationDigitalNetworkWrapper.getNextVideo(10114, 565) }
        assertNull(nextEpisode)
    }
}