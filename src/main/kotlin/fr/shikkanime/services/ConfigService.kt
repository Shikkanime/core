package fr.shikkanime.services

import com.google.inject.Inject
import fr.shikkanime.entities.Config
import fr.shikkanime.repositories.ConfigRepository
import io.ktor.http.*
import java.util.*

class ConfigService : AbstractService<Config, ConfigRepository>() {
    @Inject
    private lateinit var configRepository: ConfigRepository

    override fun getRepository() = configRepository

    fun findAllByName(name: String) = configRepository.findAllByName(name)

    fun findByName(name: String) = configRepository.findByName(name)

    fun getValueAsString(name: String) = findByName(name)?.propertyValue
    fun getValueAsInt(name: String) = findByName(name)?.propertyValue?.toIntOrNull()

    fun update(uuid: UUID, parameters: Parameters): Config? {
        val config = find(uuid) ?: return null
        parameters["value"]?.takeIf { it.isNotBlank() }?.let { config.propertyValue = it }
        return super.update(config)
    }
}