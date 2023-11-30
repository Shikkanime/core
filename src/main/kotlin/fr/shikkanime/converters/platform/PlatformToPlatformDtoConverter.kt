package fr.shikkanime.converters.platform

import fr.shikkanime.converters.AbstractConverter
import fr.shikkanime.dtos.PlatformDto
import fr.shikkanime.entities.Platform

class PlatformToPlatformDtoConverter : AbstractConverter<Platform, PlatformDto>() {
    override fun convert(from: Platform): PlatformDto {
        return PlatformDto(
            uuid = from.uuid,
            name = from.name!!,
            url = from.url!!,
            image = from.image!!
        )
    }
}