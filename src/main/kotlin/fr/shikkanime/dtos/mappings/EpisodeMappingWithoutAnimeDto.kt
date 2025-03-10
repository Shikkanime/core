package fr.shikkanime.dtos.mappings

import fr.shikkanime.dtos.PlatformDto
import fr.shikkanime.dtos.enums.Status
import fr.shikkanime.dtos.variants.EpisodeVariantWithoutMappingDto
import fr.shikkanime.entities.enums.EpisodeType
import fr.shikkanime.entities.enums.LangType
import java.util.*

data class EpisodeMappingWithoutAnimeDto(
    val uuid: UUID,
    val releaseDateTime: String,
    val lastReleaseDateTime: String,
    val lastUpdateDateTime: String,
    var episodeType: EpisodeType,
    val season: Int,
    val number: Int,
    val duration: Long,
    val title: String?,
    val description: String?,
    val image: String,
    val variants: Set<EpisodeVariantWithoutMappingDto>? = null,
    val platforms: Set<PlatformDto>? = null,
    val langTypes: Set<LangType>? = null,
    val status: Status,
)
