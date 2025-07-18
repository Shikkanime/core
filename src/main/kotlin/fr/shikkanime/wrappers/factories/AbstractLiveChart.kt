package fr.shikkanime.wrappers.factories

import fr.shikkanime.entities.enums.Platform
import fr.shikkanime.utils.HttpRequest
import java.time.LocalDate

abstract class AbstractLiveChart {
    protected val baseUrl = "https://www.livechart.me"
    protected val httpRequest = HttpRequest()

    abstract suspend fun getAnimeIdsFromDate(date: LocalDate): Set<String>
    abstract suspend fun getStreamsForAnime(animeId: String): Map<Platform, Set<String>>
}