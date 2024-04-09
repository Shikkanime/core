package fr.shikkanime.dtos

import fr.shikkanime.dtos.enums.Status
import fr.shikkanime.entities.enums.EpisodeType
import fr.shikkanime.entities.enums.LangType
import java.util.*

data class EpisodeDto(
    val uuid: UUID?,
    val platform: PlatformDto,
    var anime: AnimeDto,
    val episodeType: EpisodeType,
    val langType: LangType,
    val hash: String,
    val releaseDateTime: String,
    val season: Int,
    val number: Int,
    val title: String?,
    val url: String,
    val image: String,
    val duration: Long,
    val description: String?,
    val uncensored: Boolean,
    val lastUpdateDateTime: String?,
    val status: Status? = null,
)
