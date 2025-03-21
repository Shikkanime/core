package fr.shikkanime.factories.impl

import fr.shikkanime.dtos.ConfigDto
import fr.shikkanime.entities.Config
import fr.shikkanime.factories.IGenericFactory

class ConfigFactory : IGenericFactory<Config, ConfigDto> {
    override fun toDto(entity: Config) = ConfigDto(
        uuid = entity.uuid,
        propertyKey = entity.propertyKey,
        propertyValue = entity.propertyValue,
    )
}