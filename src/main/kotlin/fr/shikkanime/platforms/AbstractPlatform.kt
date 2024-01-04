package fr.shikkanime.platforms

import fr.shikkanime.entities.Episode
import fr.shikkanime.entities.enums.Platform
import fr.shikkanime.platforms.configuration.PlatformConfiguration
import fr.shikkanime.utils.Constant
import fr.shikkanime.utils.ObjectParser
import kotlinx.coroutines.runBlocking
import java.io.File
import java.lang.reflect.ParameterizedType
import java.time.ZonedDateTime

abstract class AbstractPlatform<C : PlatformConfiguration<*>, K : Any, V> {
    val hashCache = mutableListOf<String>()
    var configuration: C? = null
    private var apiCache = mutableMapOf<Pair<K, ZonedDateTime>, V>()

    abstract fun getPlatform(): Platform
    abstract suspend fun fetchApiContent(key: K, zonedDateTime: ZonedDateTime): V
    abstract fun fetchEpisodes(zonedDateTime: ZonedDateTime, bypassFileContent: File? = null): List<Episode>
    abstract fun reset()

    fun getApiContent(key: K, zonedDateTime: ZonedDateTime): V {
        val entry = apiCache.entries.firstOrNull { it.key.first == key }
        if (entry == null || zonedDateTime.isEqualOrAfter(entry.key.second.plusMinutes(configuration!!.apiCheckDelayInMinutes))) {
            apiCache.remove(entry?.key)
            apiCache[Pair(key, zonedDateTime)] = runBlocking { fetchApiContent(key, zonedDateTime) }
        }
        return apiCache.entries.firstOrNull { it.key.first == key }!!.value
    }

    fun loadConfiguration(): C {
        val defaultValue = getConfigurationClass().getConstructor().newInstance()
        val file = getConfigurationFile()

        if (!file.exists()) {
            configuration = defaultValue
            return defaultValue
        }

        return try {
            println("Reading config file for ${getPlatform().name} platform")
            val fromJson = ObjectParser.fromJson(file.readText(), getConfigurationClass())
            configuration = fromJson
            fromJson
        } catch (e: Exception) {
            println("Error while reading config file for ${getPlatform().name} platform: ${e.message}")
            defaultValue
        }
    }

    open fun saveConfiguration() {
        val file = getConfigurationFile()
        if (!file.exists()) file.createNewFile()
        file.writeText(ObjectParser.toJson(configuration))
    }

    private fun ZonedDateTime.isEqualOrAfter(other: ZonedDateTime): Boolean {
        return this.isEqual(other) || this.isAfter(other)
    }

    private fun getConfigurationFile(): File {
        val folder = File(Constant.dataFolder, "config")
        if (!folder.exists()) folder.mkdirs()
        return File(folder, "${getPlatform().platformName.lowercase().replace(" ", "-")}.json")
    }

    private fun getConfigurationClass(): Class<C> {
        val type = (javaClass.genericSuperclass as ParameterizedType).actualTypeArguments[0]
        require(type is Class<*>) { "Configuration class must be a class" }
        return type as Class<C>
    }
}
