package fr.shikkanime.converters.anime_platform

import fr.shikkanime.converters.AbstractConverter
import fr.shikkanime.dtos.*
import fr.shikkanime.entities.AnimePlatform

class AnimePlatformToAnimePlatformDtoConverter : AbstractConverter<AnimePlatform, AnimePlatformDto>() {
    @Converter
    fun convert(from: AnimePlatform): AnimePlatformDto {
        return AnimePlatformDto(
            uuid = from.uuid,
            platform = convert(from.platform, PlatformDto::class.java),
            platformId = from.platformId!!,
        )
    }
}