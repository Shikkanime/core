package fr.shikkanime.services.caches

import com.google.gson.reflect.TypeToken
import com.google.inject.Inject
import fr.shikkanime.dtos.ConfigDto
import fr.shikkanime.entities.Config
import fr.shikkanime.entities.enums.ConfigPropertyKey
import fr.shikkanime.factories.impl.ConfigFactory
import fr.shikkanime.services.ConfigService
import fr.shikkanime.utils.MapCache
import fr.shikkanime.utils.MapCacheValue
import fr.shikkanime.utils.StringUtils

class ConfigCacheService : ICacheService {
    @Inject private lateinit var configService: ConfigService
    @Inject private lateinit var configFactory: ConfigFactory

    suspend fun findByName(name: String) = MapCache.getOrComputeNullableAsync(
        "ConfigCacheService.findByName",
        classes = listOf(Config::class.java),
        typeToken = object : TypeToken<MapCacheValue<ConfigDto>>() {},
        key = name,
    ) { configService.findByName(it)?.let { config -> configFactory.toDto(config) } }

    suspend fun getValueAsString(configPropertyKey: ConfigPropertyKey) = findByName(configPropertyKey.key)?.propertyValue

    suspend fun getValueAsString(configPropertyKey: ConfigPropertyKey, defaultValue: String) =
        getValueAsString(configPropertyKey)?.ifBlank { null } ?: defaultValue

    suspend fun getValueAsIntNullable(configPropertyKey: ConfigPropertyKey) =
        findByName(configPropertyKey.key)?.propertyValue?.toIntOrNull()

    suspend fun getValueAsInt(configPropertyKey: ConfigPropertyKey, defaultValue: Int) =
        getValueAsIntNullable(configPropertyKey) ?: defaultValue

    suspend fun getValueAsInt(configPropertyKey: ConfigPropertyKey) = getValueAsInt(configPropertyKey, -1)

    suspend fun getValueAsLongNullable(configPropertyKey: ConfigPropertyKey) =
        findByName(configPropertyKey.key)?.propertyValue?.toLongOrNull()

    suspend fun getValueAsLong(configPropertyKey: ConfigPropertyKey, defaultValue: Long) =
        getValueAsLongNullable(configPropertyKey) ?: defaultValue

    suspend fun getValueAsBoolean(configPropertyKey: ConfigPropertyKey) =
        findByName(configPropertyKey.key)?.propertyValue?.toBoolean() == true

    suspend fun getValueAsBoolean(configPropertyKey: ConfigPropertyKey, defaultValue: Boolean) =
        findByName(configPropertyKey.key)?.propertyValue?.toBoolean() ?: defaultValue

    suspend fun getValueAsStringList(configPropertyKey: ConfigPropertyKey) =
        findByName(configPropertyKey.key)?.propertyValue?.split(StringUtils.COMMA_STRING) ?: emptyList()
}