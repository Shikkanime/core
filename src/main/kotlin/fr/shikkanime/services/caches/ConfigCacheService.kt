package fr.shikkanime.services.caches

import com.google.inject.Inject
import fr.shikkanime.entities.Config
import fr.shikkanime.entities.enums.ConfigPropertyKey
import fr.shikkanime.services.ConfigService
import fr.shikkanime.utils.MapCache

class ConfigCacheService : AbstractCacheService {
    @Inject
    private lateinit var configService: ConfigService

    private val cache = MapCache<String, List<Config>>(
        classes = listOf(Config::class.java),
        defaultKeys = listOf("all")
    ) {
        configService.findAll()
    }

    private fun findByName(name: String) = cache["all"]?.find { it.propertyKey == name }

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