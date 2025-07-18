package fr.shikkanime.wrappers.factories

import fr.shikkanime.utils.HttpRequest
import org.jsoup.nodes.Document
import java.time.LocalDate

abstract class AbstractAniDBWrapper {
    data class Episode(
        val id: Int,
        val resources: Map<Int, Set<String>>,
        val number: String,
        val airdate: LocalDate
    )

    protected val httpRequest = HttpRequest()

    abstract suspend fun getAnimeTitles(): Document
    abstract suspend fun getAnimeDetails(clientId: String, animeId: Int): Document
    abstract suspend fun getEpisodesByAnime(clientId: String, animeId: Int): List<Episode>
}