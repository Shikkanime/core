package fr.shikkanime.wrappers.factories

import fr.shikkanime.entities.enums.Platform
import java.time.LocalDate

abstract class AbstractLiveChartWrapper {
    protected val baseUrl = "https://www.livechart.me"

    abstract suspend fun getAnimeIdsFromDate(date: LocalDate): Array<String>
    abstract suspend fun getStreamsByAnimeId(animeId: String): HashMap<Platform, Set<String>>
}