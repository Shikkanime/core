package fr.shikkanime.wrappers.factories

import fr.shikkanime.entities.enums.Platform
import fr.shikkanime.utils.HttpRequest
import java.time.LocalDate

abstract class AbstractLiveChartWrapper {
    protected val baseUrl = "https://www.livechart.me"
    protected val httpRequest = HttpRequest()

    abstract suspend fun getAnimeIdsFromDate(date: LocalDate): Array<String>
    abstract suspend fun getStreamsByAnimeId(animeId: String): HashMap<Platform, Set<String>>
}