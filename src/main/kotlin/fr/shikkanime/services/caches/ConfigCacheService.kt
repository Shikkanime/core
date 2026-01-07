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

    fun findByName(name: String) = MapCache.getOrComputeNullable(
        "ConfigCacheService.findAll",
        classes = listOf(Config::class.java),
        typeToken = object : TypeToken<MapCacheValue<ConfigDto>>() {},
        key = name,
    ) { configService.findByName(it)?.let(configFactory::toDto) }

    fun getValueAsString(configPropertyKey: ConfigPropertyKey) = findByName(configPropertyKey.key)?.propertyValue

    fun getValueAsIntNullable(configPropertyKey: ConfigPropertyKey) =
        findByName(configPropertyKey.key)?.propertyValue?.toIntOrNull()

    fun getValueAsInt(configPropertyKey: ConfigPropertyKey, defaultValue: Int) =
        getValueAsIntNullable(configPropertyKey) ?: defaultValue

    fun getValueAsInt(configPropertyKey: ConfigPropertyKey) = getValueAsInt(configPropertyKey, -1)

    fun getValueAsLongNullable(configPropertyKey: ConfigPropertyKey) =
        findByName(configPropertyKey.key)?.propertyValue?.toLongOrNull()

    fun getValueAsLong(configPropertyKey: ConfigPropertyKey, defaultValue: Long) =
        getValueAsLongNullable(configPropertyKey) ?: defaultValue

    fun getValueAsBoolean(configPropertyKey: ConfigPropertyKey) =
        findByName(configPropertyKey.key)?.propertyValue?.toBoolean() == true

    fun getValueAsBoolean(configPropertyKey: ConfigPropertyKey, defaultValue: Boolean) =
        findByName(configPropertyKey.key)?.propertyValue?.toBoolean() ?: defaultValue

    fun getValueAsStringList(configPropertyKey: ConfigPropertyKey) =
        findByName(configPropertyKey.key)?.propertyValue?.split(StringUtils.COMMA_STRING) ?: emptyList()
}