package fr.shikkanime.converters.config

import fr.shikkanime.converters.AbstractConverter
import fr.shikkanime.dtos.ConfigDto
import fr.shikkanime.entities.Config

class ConfigToConfigDtoConverter : AbstractConverter<Config, ConfigDto>() {
    override fun convert(from: Config): ConfigDto {
        return ConfigDto(
            uuid = from.uuid,
            propertyKey = from.propertyKey,
            propertyValue = from.propertyValue,
        )
    }
}