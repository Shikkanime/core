package fr.shikkanime.services.caches

import com.google.inject.Inject
import fr.shikkanime.entities.Config
import fr.shikkanime.entities.enums.ConfigPropertyKey
import fr.shikkanime.services.ConfigService
import fr.shikkanime.utils.MapCache

class ConfigCacheService : AbstractCacheService {
    @Inject
    private lateinit var configService: ConfigService

    private val cache = MapCache<String, Config?>(classes = listOf(Config::class.java)) {
        configService.findByName(it)
    }

    private fun findByName(name: String) = cache[name]

    fun getValueAsString(configPropertyKey: ConfigPropertyKey) = findByName(configPropertyKey.key)?.propertyValue

    fun getValueAsIntNullable(configPropertyKey: ConfigPropertyKey) =
        findByName(configPropertyKey.key)?.propertyValue?.toIntOrNull()

    fun getValueAsInt(configPropertyKey: ConfigPropertyKey, defaultValue: Int) =
        getValueAsIntNullable(configPropertyKey) ?: defaultValue

    fun getValueAsInt(configPropertyKey: ConfigPropertyKey) = getValueAsInt(configPropertyKey, -1)

    fun getValueAsBoolean(configPropertyKey: ConfigPropertyKey, defaultValue: Boolean) =
        findByName(configPropertyKey.key)?.propertyValue?.toBoolean() ?: defaultValue

    fun getValueAsBoolean(configPropertyKey: ConfigPropertyKey) =
        getValueAsBoolean(configPropertyKey, false)
}