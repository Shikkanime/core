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

    val logger = LoggerFactory.getLogger(javaClass)
    var configuration: C? = null
    val apiCache = mutableMapOf<K, Pair<ZonedDateTime, V>>()

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

    private fun isEntryExpired(lastFetch: ZonedDateTime, currentTime: ZonedDateTime): Boolean {
        val expirationTime = lastFetch.plusMinutes(configuration!!.apiCheckDelayInMinutes)
        return currentTime.isAfterOrEqual(expirationTime)
    }

    fun getApiContent(key: K, currentTime: ZonedDateTime): V {
        val normalizedTime = currentTime.withSecond(0).withNano(0)
        val cacheEntry = apiCache[key]
        val (lastFetch, cachedValue) = cacheEntry ?: (normalizedTime to null)

        if (cachedValue == null || isEntryExpired(lastFetch, normalizedTime)) {
            val result = runCatching { runBlocking { fetchApiContent(key, normalizedTime) } }
                .getOrElse { exception ->
                    logger.warning("Error fetching API content for key $key on ${getPlatform().name}: ${exception.message}")
                    throw exception
                }

            apiCache[key] = normalizedTime to result
        }

        return apiCache[key]?.second ?: throw IllegalStateException("Cache entry for key $key should not be null here")
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
