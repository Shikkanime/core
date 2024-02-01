package fr.shikkanime.services.caches

import com.google.inject.Inject
import fr.shikkanime.entities.Config
import fr.shikkanime.entities.enums.ConfigPropertyKey
import fr.shikkanime.services.ConfigService
import fr.shikkanime.utils.MapCache

class ConfigCacheService : AbstractCacheService() {
    @Inject
    private lateinit var configService: ConfigService

    private val cache = MapCache<String, Config?>(classes = listOf(Config::class.java)) {
        configService.findByName(it)
    }

    private fun findByName(name: String) = cache[name]

    fun getValueAsString(configPropertyKey: ConfigPropertyKey) = findByName(configPropertyKey.key)?.propertyValue

    fun getValueAsInt(configPropertyKey: ConfigPropertyKey, defaultValue: Int) =
        findByName(configPropertyKey.key)?.propertyValue?.toIntOrNull() ?: defaultValue

    fun getValueAsInt(configPropertyKey: ConfigPropertyKey) = getValueAsInt(configPropertyKey, -1)

    fun getValueAsBoolean(configPropertyKey: ConfigPropertyKey) =
        findByName(configPropertyKey.key)?.propertyValue?.toBoolean() ?: false
}