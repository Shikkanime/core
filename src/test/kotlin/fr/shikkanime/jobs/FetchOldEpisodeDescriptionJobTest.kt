package fr.shikkanime.jobs

import fr.shikkanime.utils.HttpRequest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class FetchOldEpisodeDescriptionJobTest {
    private val fetchOldEpisodeDescriptionJob = FetchOldEpisodeDescriptionJob()

    @Test
    fun normalizeUrl() {
        assertEquals("GMKUXPD53", fetchOldEpisodeDescriptionJob.normalizeUrl("https://www.crunchyroll.com/fr/watch/GMKUXPD53/"))
        assertEquals("G14U415N4", fetchOldEpisodeDescriptionJob.normalizeUrl("https://www.crunchyroll.com/fr/watch/G14U415N4/the-panicked-foolish-angel-and-demon"))
        assertEquals("G14U415D2", fetchOldEpisodeDescriptionJob.normalizeUrl("https://www.crunchyroll.com/fr/watch/G14U415D2/natsukawa-senpai-is-super-good-looking"))
        assertEquals("G8WUN158J", fetchOldEpisodeDescriptionJob.normalizeUrl("https://www.crunchyroll.com/fr/watch/G8WUN158J/"))
        assertEquals("GEVUZD021", fetchOldEpisodeDescriptionJob.normalizeUrl("https://www.crunchyroll.com/fr/watch/GEVUZD021/becoming-a-three-star-chef"))
        assertEquals("GK9U3KWN4", fetchOldEpisodeDescriptionJob.normalizeUrl("https://www.crunchyroll.com/fr/watch/GK9U3KWN4/yukis-world"))
    }

    @Test
    fun bug() {
        val normalizeUrl = "https://www.crunchyroll.com/fr/media-918855"

        val lastPage = HttpRequest().use {
            it.getBrowser(normalizeUrl)
            it.lastPageUrl!!
        }

        val id = fetchOldEpisodeDescriptionJob.normalizeUrl(lastPage)
        assertEquals("GVWU07GP0", id)
    }
}