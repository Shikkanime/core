package fr.shikkanime.converters.platform

import fr.shikkanime.converters.AbstractConverter
import fr.shikkanime.dtos.PlatformDto
import fr.shikkanime.entities.enums.Platform

class PlatformToPlatformDtoConverter : AbstractConverter<Platform, PlatformDto>() {
    override fun convert(from: Platform): PlatformDto {
        return PlatformDto(
            id = from.name,
            name = from.platformName,
            url = from.url,
            image = from.image
        )
    }
}