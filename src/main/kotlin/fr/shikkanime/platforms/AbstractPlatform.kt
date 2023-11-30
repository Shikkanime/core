package fr.shikkanime.platforms

import fr.shikkanime.entities.Platform
import java.time.ZonedDateTime

abstract class AbstractPlatform {
    data class Api(
        val lastCheck: ZonedDateTime,
        val content: Map<String, String> = emptyMap(),
    )

    private var apiCache: Api? = null

    abstract val platform: Platform

    protected abstract fun getCountries(): List<String>
    abstract suspend fun fetchApiContent(zonedDateTime: ZonedDateTime): Api
    abstract suspend fun fetchEpisodes(zonedDateTime: ZonedDateTime): List<String>
    abstract fun reset()

    suspend fun getApiContent(countryCode: String, zonedDateTime: ZonedDateTime, delayHours: Long? = null): String {
        if (apiCache == null) {
            apiCache = fetchApiContent(zonedDateTime)
        }

        val plusHours = apiCache!!.lastCheck.plusHours(delayHours ?: 0)

        if (zonedDateTime.isEqual(plusHours) || zonedDateTime.isAfter(plusHours)) {
            apiCache = fetchApiContent(zonedDateTime)
        }

        return apiCache!!.content[countryCode]!!
    }
}
