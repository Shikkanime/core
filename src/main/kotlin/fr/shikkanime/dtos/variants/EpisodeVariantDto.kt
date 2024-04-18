package fr.shikkanime.dtos.variants

import fr.shikkanime.dtos.EpisodeMappingDto
import fr.shikkanime.dtos.PlatformDto
import java.util.*

data class EpisodeVariantDto(
    val uuid: UUID,
    val mapping: EpisodeMappingDto,
    val releaseDateTime: String,
    val platform: PlatformDto,
    val audioLocale: String,
    val identifier: String,
    val url: String,
    val uncensored: Boolean,
)
