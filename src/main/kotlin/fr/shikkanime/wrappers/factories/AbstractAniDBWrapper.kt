package fr.shikkanime.wrappers.factories

import fr.shikkanime.utils.HttpRequest
import java.io.Serializable
import java.time.LocalDate

abstract class AbstractAniDBWrapper {
    data class Episode(
        val id: String,
        val titles: Set<String>,
        val aired: LocalDate,
    ) : Serializable

    protected val baseUrl = "https://anidb.net"
    protected val httpRequest = HttpRequest()

    abstract suspend fun getEpisodesByMediaId(id: Int): Array<Episode>
}