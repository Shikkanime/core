package fr.shikkanime.services.caches

import com.google.inject.Inject
import fr.shikkanime.entities.Config
import fr.shikkanime.entities.enums.ConfigPropertyKey
import fr.shikkanime.services.ConfigService
import fr.shikkanime.utils.MapCache
import fr.shikkanime.utils.StringUtils

class ConfigCacheService : ICacheService {
    @Inject private lateinit var configService: ConfigService

    fun findByName(name: String) = MapCache.getOrCompute(
        "ConfigCacheService.findAll",
        classes = listOf(Config::class.java),
        key = StringUtils.EMPTY_STRING,
    ) { configService.findAll() }
        .find { it.propertyKey == name }

    fun getValueAsString(configPropertyKey: ConfigPropertyKey) = findByName(configPropertyKey.key)?.propertyValue

    fun getValueAsIntNullable(configPropertyKey: ConfigPropertyKey) =
        findByName(configPropertyKey.key)?.propertyValue?.toIntOrNull()

    fun getValueAsInt(configPropertyKey: ConfigPropertyKey, defaultValue: Int) =
        getValueAsIntNullable(configPropertyKey) ?: defaultValue

    fun getValueAsInt(configPropertyKey: ConfigPropertyKey) = getValueAsInt(configPropertyKey, -1)

    fun getValueAsBoolean(configPropertyKey: ConfigPropertyKey) =
        findByName(configPropertyKey.key)?.propertyValue?.toBoolean() == true

    fun getValueAsBoolean(configPropertyKey: ConfigPropertyKey, defaultValue: Boolean) =
        findByName(configPropertyKey.key)?.propertyValue?.toBoolean() ?: defaultValue

    fun getValueAsStringList(configPropertyKey: ConfigPropertyKey) =
        findByName(configPropertyKey.key)?.propertyValue?.split(",") ?: emptyList()
}