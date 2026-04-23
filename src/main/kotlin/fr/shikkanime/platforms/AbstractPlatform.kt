package fr.shikkanime.platforms

import com.google.inject.Inject
import fr.shikkanime.entities.enums.CountryCode
import fr.shikkanime.entities.enums.EpisodeType
import fr.shikkanime.entities.enums.ImageType
import fr.shikkanime.entities.enums.Platform
import fr.shikkanime.platforms.configuration.PlatformConfiguration
import fr.shikkanime.services.caches.AnimePlatformCacheService
import fr.shikkanime.services.caches.ConfigCacheService
import fr.shikkanime.utils.*
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
        val variantOf: String? = null
    ) {
        fun getIdentifier() = StringUtils.getIdentifier(countryCode, platform, id, audioLocale, uncensored)
    }

    data class CacheEntry<V>(
        val lastFetch: ZonedDateTime,
        val cachedValue: V?,
        val hasError: Boolean
    )

    @Inject protected lateinit var configCacheService: ConfigCacheService
    @Inject protected lateinit var animePlatformCacheService: AnimePlatformCacheService

    val logger = LoggerFactory.getLogger(javaClass)
    var configuration: C? = null
    val apiCache = mutableMapOf<K, CacheEntry<V>>()

    private var lastLatestShowsFetch: ZonedDateTime? = null
    private var hasLatestShowsFetchError = false

    protected fun isBlacklisted(name: String): Boolean {
        val blacklist = configuration?.blacklistedSimulcasts?.takeIfNotEmpty()?.toSet() ?: return false
        return name.lowercase() in blacklist || StringUtils.getShortName(name).lowercase() in blacklist
    }

    abstract fun getPlatform(): Platform
    abstract fun getConfigurationClass(): Class<C>
    abstract suspend fun fetchApiContent(key: K, zonedDateTime: ZonedDateTime): V
    abstract suspend fun fetchEpisodes(zonedDateTime: ZonedDateTime, bypassFileContent: File? = null): List<Episode>

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

    private fun shouldFetchLatestShows(currentTime: ZonedDateTime): Boolean {
        val delayMinutes = configuration!!.apiCheckDelayInMinutes

        if (lastLatestShowsFetch == null || hasLatestShowsFetchError || delayMinutes <= 0) {
            return true
        }

        return currentTime.minute.toLong() % delayMinutes == 0L && currentTime.second == 0
    }

    suspend fun getApiContent(key: K, currentTime: ZonedDateTime): V {
        if (shouldFetchContent(key, currentTime)) {
            val result = runCatching { fetchApiContent(key, currentTime) }
                .fold(
                    onSuccess = { fetchResult ->
                        // Success: cached with hasError = false
                        apiCache[key] = CacheEntry(currentTime, fetchResult, false)
                        fetchResult
                    },
                    onFailure = { exception ->
                        logger.warning("Error fetching API content for key $key on ${getPlatform().name}: ${exception.message}")

                        // In case of error, mark hasError = true and keep the old value if it exists
                        val currentCache = apiCache[key]
                        val existingValue = currentCache?.cachedValue

                        apiCache[key] = CacheEntry(currentTime, existingValue, true)

                        // If we have a cached value, return it, otherwise propagate the error
                        existingValue ?: throw exception
                    }
                )

            return result
        }

        // Return the cached value
        return apiCache[key]?.cachedValue ?: throw IllegalStateException("Cache entry for key $key should not be null here")
    }

    suspend fun getLatestShows(currentTime: ZonedDateTime, block: suspend () -> Unit) {
        if (shouldFetchLatestShows(currentTime)) {
            runCatching { block() }
                .fold(
                    onSuccess = { fetchResult ->
                        lastLatestShowsFetch = currentTime
                        hasLatestShowsFetchError = false
                        fetchResult
                    },
                    onFailure = { exception ->
                        hasLatestShowsFetchError = true
                        logger.log(Level.WARNING, "Error fetching latest shows for ${getPlatform().name}", exception)
                    }
                )
        }
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

    fun containsAnimeSimulcastConfiguration(name: String) = configuration?.simulcasts?.any { it.name.equals(name, ignoreCase = true) } ?: false

    fun updateAnimeSimulcastConfiguration(name: String) {
        configuration?.simulcasts?.find { it.name.equals(name, ignoreCase = true) }?.let {
            it.lastUsageDateTime = ZonedDateTime.now().withUTCString()
            saveConfiguration()
        }
    }
}
