package fr.shikkanime.services.caches

import com.google.inject.Inject
import fr.shikkanime.entities.Config
import fr.shikkanime.entities.enums.ConfigPropertyKey
import fr.shikkanime.services.ConfigService
import fr.shikkanime.utils.MapCache

class ConfigCacheService : AbstractCacheService {
    companion object {
        private const val DEFAULT_ALL_KEY = "all"
    }

    @Inject
    private lateinit var configService: ConfigService

    private val cache = MapCache(
        "ConfigCacheService.cache",
        classes = listOf(Config::class.java),
        fn = { listOf(DEFAULT_ALL_KEY) }
    ) {
        configService.findAll()
    }

    fun findByName(name: String) = cache[DEFAULT_ALL_KEY]?.find { it.propertyKey == name }

    fun getValueAsString(configPropertyKey: ConfigPropertyKey) = findByName(configPropertyKey.key)?.propertyValue

    fun getValueAsIntNullable(configPropertyKey: ConfigPropertyKey) =
        findByName(configPropertyKey.key)?.propertyValue?.toIntOrNull()

    fun getValueAsInt(configPropertyKey: ConfigPropertyKey, defaultValue: Int) =
        getValueAsIntNullable(configPropertyKey) ?: defaultValue

    fun getValueAsInt(configPropertyKey: ConfigPropertyKey) = getValueAsInt(configPropertyKey, -1)

    fun getValueAsBoolean(configPropertyKey: ConfigPropertyKey) =
        findByName(configPropertyKey.key)?.propertyValue?.toBoolean() == true

    fun getValueAsStringList(configPropertyKey: ConfigPropertyKey) =
        findByName(configPropertyKey.key)?.propertyValue?.split(",") ?: emptyList()
}