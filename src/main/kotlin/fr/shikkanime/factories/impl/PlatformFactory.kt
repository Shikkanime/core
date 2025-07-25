package fr.shikkanime.factories.impl

import fr.shikkanime.dtos.PlatformDto
import fr.shikkanime.entities.enums.Platform
import fr.shikkanime.factories.IGenericFactory

class PlatformFactory : IGenericFactory<Platform, PlatformDto> {
    override fun toDto(entity: Platform) = PlatformDto(
        id = entity.name,
        name = entity.platformName,
        url = entity.url,
        image = entity.image,
        isStreaming = entity.isStreamingPlatform
    )

    override fun toEntity(dto: PlatformDto) = Platform.findByName(dto.name)!!
}