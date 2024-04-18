package fr.shikkanime.services

import com.google.inject.Inject
import fr.shikkanime.dtos.ConfigDto
import fr.shikkanime.entities.Config
import fr.shikkanime.repositories.ConfigRepository
import fr.shikkanime.utils.MapCache
import java.util.*

class ConfigService : AbstractService<Config, ConfigRepository>() {
    @Inject
    private lateinit var configRepository: ConfigRepository

    override fun getRepository() = configRepository

    fun findAllByName(name: String) = configRepository.findAllByName(name)

    fun findByName(name: String) = configRepository.findByName(name)

    fun update(uuid: UUID, configDto: ConfigDto): Config? {
        val config = find(uuid) ?: return null

        if (config.propertyValue != configDto.propertyValue) {
            config.propertyValue = configDto.propertyValue
            MapCache.invalidate(Config::class.java)
            return super.update(config)
        }

        return config
    }
}