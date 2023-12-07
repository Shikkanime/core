package fr.shikkanime.platforms

import fr.shikkanime.entities.Country
import fr.shikkanime.entities.Platform
import fr.shikkanime.services.CountryService
import fr.shikkanime.services.PlatformService
import fr.shikkanime.utils.Constant
import jakarta.inject.Inject
import kotlinx.coroutines.runBlocking
import java.io.File
import java.time.ZonedDateTime

abstract class AbstractPlatform<C : PlatformConfiguration> {
    data class Api(
        val lastCheck: ZonedDateTime,
        val content: Map<String, String> = emptyMap(),
    )

    @Inject
    protected lateinit var platformService: PlatformService

    @Inject
    protected lateinit var countryService: CountryService

    var configuration: C? = null
    private var apiCache: Api? = null

    protected abstract fun getConfigurationClass(): Class<C>
    abstract fun getPlatform(): Platform
    abstract suspend fun fetchApiContent(zonedDateTime: ZonedDateTime): Api
    abstract fun fetchEpisodes(zonedDateTime: ZonedDateTime): List<String>
    abstract fun reset()

    protected fun getCountries() = countryService.findAllByCode(configuration!!.availableCountries)

    fun getApiContent(country: Country, zonedDateTime: ZonedDateTime): String {
        var hasFetch = false

        if (apiCache == null) {
            apiCache = runBlocking { fetchApiContent(zonedDateTime) }
            hasFetch = true
        }

        val plusHours = apiCache!!.lastCheck.plusMinutes(configuration!!.apiCheckDelayInMinutes)

        if (!hasFetch && zonedDateTime.isEqual(plusHours) || zonedDateTime.isAfter(plusHours)) {
            apiCache = runBlocking { fetchApiContent(zonedDateTime) }
        }

        return apiCache!!.content[country.countryCode]!!
    }

    fun loadConfiguration(): C {
        if (configuration != null) {
            return configuration!!
        }

        val defaultValue = getConfigurationClass().getConstructor().newInstance()
        val file = getConfigurationFile()

        if (!file.exists()) {
            configuration = defaultValue
            return defaultValue
        }

        try {
            println("Reading config file for ${getPlatform().name} platform")
            val fromJson = Constant.gson.fromJson(file.readText(), getConfigurationClass())
            configuration = fromJson
            return fromJson
        } catch (e: Exception) {
            println("Error while reading config file for ${getPlatform().name} platform: ${e.message}")
            return defaultValue
        }
    }

    fun saveConfiguration() {
        val file = getConfigurationFile()

        if (!file.exists()) {
            file.parentFile.mkdirs()
            file.createNewFile()
        }

        file.writeText(Constant.gson.toJson(configuration))
        apiCache = null
    }

    private fun getConfigurationFile(): File {
        return File("config/${getPlatform().name!!.lowercase().replace(" ", "-")}.json")
    }
}
