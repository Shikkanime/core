package fr.shikkanime.jobs

import fr.shikkanime.entities.enums.Platform
import fr.shikkanime.utils.HttpRequest
import fr.shikkanime.wrappers.AnimationDigitalNetworkWrapper
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class FetchDeprecatedEpisodeJobTest {
    private val fetchDeprecatedEpisodeJob = FetchDeprecatedEpisodeJob()

    @Test
    fun normalizeUrl() {
        assertEquals(
            "GMKUXPD53",
            fetchDeprecatedEpisodeJob.normalizeUrl("https://www.crunchyroll.com/fr/watch/GMKUXPD53/")
        )
        assertEquals(
            "G14U415N4",
            fetchDeprecatedEpisodeJob.normalizeUrl("https://www.crunchyroll.com/fr/watch/G14U415N4/the-panicked-foolish-angel-and-demon")
        )
        assertEquals(
            "G14U415D2",
            fetchDeprecatedEpisodeJob.normalizeUrl("https://www.crunchyroll.com/fr/watch/G14U415D2/natsukawa-senpai-is-super-good-looking")
        )
        assertEquals(
            "G8WUN158J",
            fetchDeprecatedEpisodeJob.normalizeUrl("https://www.crunchyroll.com/fr/watch/G8WUN158J/")
        )
        assertEquals(
            "GEVUZD021",
            fetchDeprecatedEpisodeJob.normalizeUrl("https://www.crunchyroll.com/fr/watch/GEVUZD021/becoming-a-three-star-chef")
        )
        assertEquals(
            "GK9U3KWN4",
            fetchDeprecatedEpisodeJob.normalizeUrl("https://www.crunchyroll.com/fr/watch/GK9U3KWN4/yukis-world")
        )
    }

    @Test
    fun bug() {
        val normalizeUrl = "https://www.crunchyroll.com/fr/media-918855"

        val lastPage = HttpRequest().use {
            it.getBrowser(normalizeUrl)
            it.lastPageUrl!!
        }

        val id = fetchDeprecatedEpisodeJob.normalizeUrl(lastPage)
        assertEquals("GVWU07GP0", id)
    }

    @Test
    fun normalizeDescription() {
        val content = runBlocking { AnimationDigitalNetworkWrapper.getShowVideo(24108) }
        assertEquals(
            "Alors qu'ils sont en route pour leur voyage scolaire, une classe de lycéens est invoquée dans un autre monde afin de participer à une lutte sanguinaire pour devenir des « sages », la classe dirigeante de ce nouveau monde. Chaque élève se voit attribuer un pouvoir, sauf Yogiri et Tomochika, qui ont été laissés pour mort par leurs camarades…",
            fetchDeprecatedEpisodeJob.normalizeDescription(Platform.ANIM, content)
        )
    }
}