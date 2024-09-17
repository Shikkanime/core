package fr.shikkanime.dtos

import java.util.UUID

data class AnimePlatformDto(
    val uuid: UUID?,
    val platform: PlatformDto,
    val platformId: String,
)
