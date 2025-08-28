package fr.shikkanime.platforms

import fr.shikkanime.entities.enums.CountryCode
import fr.shikkanime.entities.enums.EpisodeType
import fr.shikkanime.entities.enums.ImageType
import fr.shikkanime.entities.enums.Platform
import fr.shikkanime.platforms.configuration.PlatformConfiguration
import fr.shikkanime.utils.*
import kotlinx.coroutines.runBlocking
import java.io.File
import java.time.ZonedDateTime
import java.util.logging.Level

abstract class AbstractPlatform<C : PlatformConfiguration<*>, K : Any, V> {
    data class Episode(
        val countryCode: CountryCode,
        val animeId: String,
        var anime: String,
        val animeAttachments: Map<ImageType, String>,
        val animeDescription: String?,
        var releaseDateTime: ZonedDateTime,
        var episodeType: EpisodeType,
        val seasonId: String,
        var season: Int,
        var number: Int,
        val duration: Long,
        var title: String?,
        var description: String?,
        val image: String,
        val platform: Platform,
        val audioLocale: String,
        val id: String,
        val url: String,
        val uncensored: Boolean,
        val original: Boolean,
    ) {
        fun getIdentifier() = StringUtils.getIdentifier(countryCode, platform, id, audioLocale, uncensored)
    }

    data class CacheEntry<V>(
        val lastFetch: ZonedDateTime,
        val cachedValue: V?,
        val hasError: Boolean
    )

    val logger = LoggerFactory.getLogger(javaClass)
    var configuration: C? = null
    val apiCache = mutableMapOf<K, CacheEntry<V>>()

    protected fun isBlacklisted(name: String): Boolean {
        val shortName = StringUtils.getShortName(name)
        val blacklist = configuration!!.blacklistedSimulcasts.toSet()
        return name in blacklist || name.lowercase() in blacklist || shortName in blacklist || shortName.lowercase() in blacklist
    }

    abstract fun getPlatform(): Platform
    abstract fun getConfigurationClass(): Class<C>
    abstract suspend fun fetchApiContent(key: K, zonedDateTime: ZonedDateTime): V
    abstract fun fetchEpisodes(zonedDateTime: ZonedDateTime, bypassFileContent: File? = null): List<Episode>

    fun reset() {
        apiCache.clear()
    }

    private fun shouldFetchContent(key: K, currentTime: ZonedDateTime): Boolean {
        val cacheEntry = apiCache[key] ?: return true
        val delayMinutes = configuration!!.apiCheckDelayInMinutes

        if (cacheEntry.hasError || cacheEntry.cachedValue == null || delayMinutes <= 0) {
            return true
        }

        return currentTime.minute.toLong() % delayMinutes == 0L && currentTime.second == 0
    }

    fun getApiContent(key: K, currentTime: ZonedDateTime): V {
        if (shouldFetchContent(key, currentTime)) {
            val result = runCatching { runBlocking { fetchApiContent(key, currentTime) } }
                .fold(
                    onSuccess = { fetchResult ->
                        // Succès : on met en cache avec hasError = false
                        apiCache[key] = CacheEntry(currentTime, fetchResult, false)
                        fetchResult
                    },
                    onFailure = { exception ->
                        logger.warning("Error fetching API content for key $key on ${getPlatform().name}: ${exception.message}")

                        // En cas d'erreur, on marque hasError = true et on garde l'ancienne valeur si elle existe
                        val currentCache = apiCache[key]
                        val existingValue = currentCache?.cachedValue

                        apiCache[key] = CacheEntry(currentTime, existingValue, true)

                        // Si on a une valeur en cache, on la retourne, sinon on propage l'erreur
                        existingValue ?: throw exception
                    }
                )

            return result
        }

        // On retourne la valeur en cache
        return apiCache[key]?.cachedValue ?: throw IllegalStateException("Cache entry for key $key should not be null here")
    }

    fun loadConfiguration(): C {
        val defaultValue = getConfigurationClass().getConstructor().newInstance()
        val file = getConfigurationFile()

        if (!file.exists()) {
            configuration = defaultValue
            return defaultValue
        }

        return try {
            logger.info("Reading config file for ${getPlatform().name} platform")
            val fromJson = ObjectParser.fromJson(file.readText(), getConfigurationClass())
            configuration = fromJson
            fromJson
        } catch (e: Exception) {
            logger.log(Level.SEVERE, "Error while reading config file for ${getPlatform().name} platform", e)
            defaultValue
        }
    }

    open fun saveConfiguration() {
        val file = getConfigurationFile()
        if (!file.exists()) file.createNewFile()
        file.writeText(ObjectParser.toJson(configuration))
    }

    private fun getConfigurationFile() =
        File(Constant.configFolder, "${getPlatform().platformName.lowercase().replace(StringUtils.SPACE_STRING, StringUtils.DASH_STRING)}.json")

    fun containsAnimeSimulcastConfiguration(name: String) = configuration!!.simulcasts.any { it.name.lowercase() == name.lowercase() }

    fun updateAnimeSimulcastConfiguration(name: String) {
        configuration!!.simulcasts.find { it.name.lowercase() == name.lowercase() }?.let {
            it.lastUsageDateTime = ZonedDateTime.now().withUTCString()
            saveConfiguration()
        }
    }
}
