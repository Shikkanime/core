package fr.shikkanime.dtos

import java.util.*

data class AnimePlatformDto(
    val uuid: UUID?,
    val platform: PlatformDto,
    val platformId: String,
    val lastValidateDateTime: String?,
)
