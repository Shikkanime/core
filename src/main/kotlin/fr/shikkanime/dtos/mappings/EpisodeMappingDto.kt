package fr.shikkanime.dtos.mappings

import fr.shikkanime.dtos.animes.AnimeDto
import fr.shikkanime.dtos.PlatformDto
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
    var episodeType: EpisodeType,
    val season: Int,
    val number: Int,
    val duration: Long,
    val title: String?,
    val description: String?,
    val image: String,
    val variants: List<EpisodeVariantWithoutMappingDto>? = null,
    val platforms: List<PlatformDto>? = null,
    val langTypes: List<LangType>? = null,
    val status: Status,
)
