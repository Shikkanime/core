package fr.shikkanime.services

import com.google.inject.Inject
import fr.shikkanime.dtos.ConfigDto
import fr.shikkanime.entities.Config
import fr.shikkanime.entities.TraceAction
import fr.shikkanime.repositories.ConfigRepository
import java.util.*

class ConfigService : AbstractService<Config, ConfigRepository>() {
    @Inject private lateinit var configRepository: ConfigRepository
    @Inject private lateinit var traceActionService: TraceActionService

    override fun getRepository() = configRepository

    fun findAllByName(name: String) = configRepository.findAllByName(name)

    fun update(uuid: UUID, configDto: ConfigDto): Config? {
        val config = find(uuid) ?: return null

        if (config.propertyValue == configDto.propertyValue) {
            return config
        }

        config.propertyValue = configDto.propertyValue
        val updated = super.update(config)
        traceActionService.createTraceAction(config, TraceAction.Action.UPDATE)
        return updated
    }
}