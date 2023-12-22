package fr.shikkanime.platforms

import fr.shikkanime.entities.Episode
import fr.shikkanime.entities.enums.Platform
import fr.shikkanime.utils.Constant
import fr.shikkanime.utils.ObjectParser
import kotlinx.coroutines.runBlocking
import java.io.File
import java.time.ZonedDateTime

abstract class AbstractPlatform<C : PlatformConfiguration, K : Any, V> {
    val hashCache = mutableListOf<String>()
    var configuration: C? = null
    private var apiCache = mutableMapOf<Pair<K, ZonedDateTime>, V>()

    protected abstract fun getConfigurationClass(): Class<C>
    abstract fun getPlatform(): Platform
    abstract suspend fun fetchApiContent(key: K, zonedDateTime: ZonedDateTime): V
    abstract fun fetchEpisodes(zonedDateTime: ZonedDateTime, bypassFileContent: File? = null): List<Episode>
    abstract fun reset()

    fun getApiContent(key: K, zonedDateTime: ZonedDateTime): V {
        if (apiCache.none { it.key.first == key }) {
            apiCache[Pair(key, zonedDateTime)] = runBlocking { fetchApiContent(key, zonedDateTime) }
        }

        val entry = apiCache.entries.firstOrNull { it.key.first == key }!!
        val plusMinutes = entry.key.second.plusMinutes(configuration!!.apiCheckDelayInMinutes)

        if (zonedDateTime.isEqual(plusMinutes) || zonedDateTime.isAfter(plusMinutes)) {
            apiCache.remove(entry.key)
            apiCache[Pair(key, zonedDateTime)] = runBlocking { fetchApiContent(key, zonedDateTime) }
        }

        return apiCache.entries.firstOrNull { it.key.first == key }!!.value
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
            val fromJson = ObjectParser.fromJson(file.readText(), getConfigurationClass())
            configuration = fromJson
            return fromJson
        } catch (e: Exception) {
            println("Error while reading config file for ${getPlatform().name} platform: ${e.message}")
            return defaultValue
        }
    }

    open fun saveConfiguration() {
        val file = getConfigurationFile()

        if (!file.exists()) {
            file.createNewFile()
        }

        file.writeText(ObjectParser.toJson(configuration))
    }

    private fun getConfigurationFile(): File {
        val folder = File(Constant.dataFolder, "config")

        if (!folder.exists()) {
            folder.mkdirs()
        }

        return File(folder, "${getPlatform().platformName.lowercase().replace(" ", "-")}.json")
    }
}
