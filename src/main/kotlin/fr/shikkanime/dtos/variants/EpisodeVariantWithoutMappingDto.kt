package fr.shikkanime.dtos.variants

import fr.shikkanime.dtos.PlatformDto
import java.util.*

data class EpisodeVariantWithoutMappingDto(
    val uuid: UUID,
    val releaseDateTime: String,
    val platform: PlatformDto,
    val audioLocale: String,
    val identifier: String,
    val url: String,
    val uncensored: Boolean,
)
