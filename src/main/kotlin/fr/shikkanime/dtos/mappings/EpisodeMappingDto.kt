package fr.shikkanime.dtos.mappings

import fr.shikkanime.dtos.EpisodeSourceDto
import fr.shikkanime.dtos.PlatformDto
import fr.shikkanime.dtos.animes.AnimeDto
import fr.shikkanime.dtos.variants.EpisodeVariantDto
import fr.shikkanime.entities.enums.EpisodeType
import fr.shikkanime.entities.enums.LangType
import java.io.Serializable
import java.util.*

data class EpisodeMappingDto(
    val uuid: UUID?,
    var anime: AnimeDto?,
    val releaseDateTime: String,
    val lastReleaseDateTime: String,
    val lastUpdateDateTime: String,
    var episodeType: EpisodeType,
    val season: Int,
    val number: Int,
    val duration: Long,
    val title: String?,
    val description: String?,
    val variants: Set<EpisodeVariantDto>? = null,
    @Deprecated("Use sources instead") val platforms: Set<PlatformDto>? = null,
    @Deprecated("Use sources instead") val langTypes: Set<LangType>? = null,
    var image: String? = null,
    val sources: Set<EpisodeSourceDto>,
) : Serializable
