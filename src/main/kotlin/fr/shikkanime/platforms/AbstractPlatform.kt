package fr.shikkanime.platforms

import fr.shikkanime.entities.enums.CountryCode
import fr.shikkanime.entities.enums.EpisodeType
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
        val animeImage: String,
        val animeBanner: String,
        val animeDescription: String?,
        val releaseDateTime: ZonedDateTime,
        val episodeType: EpisodeType,
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
    private val apiCache = mutableMapOf<Pair<K, ZonedDateTime>, V>()
    val hashCache = mutableSetOf<String>()

    abstract fun getPlatform(): Platform
    abstract fun getConfigurationClass(): Class<C>
    abstract suspend fun fetchApiContent(key: K, zonedDateTime: ZonedDateTime): V
    abstract fun fetchEpisodes(zonedDateTime: ZonedDateTime, bypassFileContent: File? = null): List<Episode>

    fun reset() {
        apiCache.clear()
    }

    fun getApiContent(key: K, zonedDateTime: ZonedDateTime): V {
        val entry = apiCache.entries.firstOrNull { it.key.first == key }

        if (entry == null || zonedDateTime.isEqualOrAfter(entry.key.second.plusMinutes(configuration!!.apiCheckDelayInMinutes))) {
            val values = runBlocking { fetchApiContent(key, zonedDateTime) }

            if (values == null) {
                logger.warning("API content is null for $key")
                return values
            }

            apiCache.remove(entry?.key)
            apiCache[Pair(key, zonedDateTime)] = values
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
        File(Constant.configFolder, "${getPlatform().platformName.lowercase().replace(" ", "-")}.json")
}
