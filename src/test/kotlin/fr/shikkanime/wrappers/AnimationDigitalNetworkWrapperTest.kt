package fr.shikkanime.wrappers

import fr.shikkanime.entities.enums.CountryCode
import fr.shikkanime.wrappers.impl.caches.AnimationDigitalNetworkCachedWrapper
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class AnimationDigitalNetworkWrapperTest {
    @Test
    suspend fun getPreviousEpisode() {
        val previousEpisode = AnimationDigitalNetworkCachedWrapper.getPreviousEpisode(CountryCode.FR.locale, 1160, 26664)
        assertNotNull(previousEpisode)
        assertEquals(26663, previousEpisode?.id)
    }

    @Test
    suspend fun getUpNext() {
        val nextEpisode = AnimationDigitalNetworkCachedWrapper.getNextEpisode(CountryCode.FR.locale, 1160, 26664)
        assertNotNull(nextEpisode)
        assertEquals(26665, nextEpisode?.id)
    }

    @Test
    suspend fun `getPreviousEpisode #2`() {
        val previousEpisode = AnimationDigitalNetworkCachedWrapper.getPreviousEpisode(CountryCode.FR.locale, 565, 10114)
        assertNotNull(previousEpisode)
        assertEquals(10113, previousEpisode?.id)
    }

    @Test
    suspend fun `getUpNext #2`() {
        val nextEpisode = AnimationDigitalNetworkCachedWrapper.getNextEpisode(CountryCode.FR.locale, 565, 10114)
        assertNull(nextEpisode)
    }
}