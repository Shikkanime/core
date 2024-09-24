package fr.shikkanime.converters.config

import fr.shikkanime.converters.AbstractConverter
import fr.shikkanime.dtos.ConfigDto
import fr.shikkanime.entities.Config
import fr.shikkanime.utils.StringUtils

class ConfigToConfigDtoConverter : AbstractConverter<Config, ConfigDto>() {
    @Converter
    fun convert(from: Config): ConfigDto {
        return ConfigDto(
            uuid = from.uuid,
            propertyKey = from.propertyKey,
            propertyValue = from.propertyValue?.let { StringUtils.sanitizeXSS(it) },
        )
    }
}