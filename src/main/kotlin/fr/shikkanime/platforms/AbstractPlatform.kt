package fr.shikkanime.platforms

import fr.shikkanime.entities.Country
import fr.shikkanime.entities.Platform
import fr.shikkanime.services.CountryService
import fr.shikkanime.services.PlatformService
import jakarta.inject.Inject
import java.time.ZonedDateTime

abstract class AbstractPlatform {
    data class Api(
        val lastCheck: ZonedDateTime,
        val content: Map<Country, String> = emptyMap(),
    )

    @Inject
    protected lateinit var platformService: PlatformService

    @Inject
    protected lateinit var countryService: CountryService

    private var apiCache: Api? = null

    abstract fun getPlatform(): Platform
    protected abstract fun getCountries(): List<Country>
    abstract suspend fun fetchApiContent(zonedDateTime: ZonedDateTime): Api
    abstract suspend fun fetchEpisodes(zonedDateTime: ZonedDateTime): List<String>
    abstract fun reset()

    suspend fun getApiContent(country: Country, zonedDateTime: ZonedDateTime, delayHours: Long? = null): String {
        if (apiCache == null) {
            apiCache = fetchApiContent(zonedDateTime)
        }

        val plusHours = apiCache!!.lastCheck.plusHours(delayHours ?: 0)

        if (zonedDateTime.isEqual(plusHours) || zonedDateTime.isAfter(plusHours)) {
            apiCache = fetchApiContent(zonedDateTime)
        }

        return apiCache!!.content[country]!!
    }
}
