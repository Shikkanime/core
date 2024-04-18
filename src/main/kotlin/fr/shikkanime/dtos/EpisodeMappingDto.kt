package fr.shikkanime.dtos

import fr.shikkanime.dtos.enums.Status
import fr.shikkanime.dtos.variants.EpisodeVariantWithoutMappingDto
import fr.shikkanime.entities.enums.EpisodeType
import fr.shikkanime.entities.enums.LangType
import java.util.*

data class EpisodeMappingDto(
    val uuid: UUID,
    val anime: AnimeDto,
    val releaseDateTime: String,
    val lastReleaseDateTime: String,
    val lastUpdateDateTime: String,
    val episodeType: EpisodeType,
    val season: Int,
    val number: Int,
    val duration: Long,
    val title: String?,
    val description: String?,
    val image: String,
    val variants: List<EpisodeVariantWithoutMappingDto>?,
    val platforms: Set<PlatformDto>? = null,
    val langTypes: Set<LangType>? = null,
    val status: Status,
)
