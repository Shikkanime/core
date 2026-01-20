package fr.shikkanime.wrappers.factories

import fr.shikkanime.utils.HttpRequest
import java.io.Serializable
import java.time.LocalDate

abstract class AbstractAnimeNewsNetworkWrapper {
    data class Media(
        val titles: Set<String>,
        val airedFrom: LocalDate,
        val airedTo: LocalDate?,
    ) : Serializable

    data class Episode(
        val id: String,
        val titles: Set<String>,
        val aired: LocalDate,
    ) : Serializable

    protected val baseUrl = "https://www.animenewsnetwork.com"
    protected val httpRequest = HttpRequest()

    abstract suspend fun getMediaById(id: Int): Media
    abstract suspend fun getEpisodesByMediaId(id: Int): Array<Episode>
}