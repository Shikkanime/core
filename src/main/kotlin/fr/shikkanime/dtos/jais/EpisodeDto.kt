package fr.shikkanime.dtos.jais

import java.io.Serializable
import java.util.*

data class EpisodeDto(
    val uuid: UUID,
    val platform: PlatformDto,
    val anime: AnimeDto,
    val episodeType: EpisodeTypeDto,
    val langType: LangTypeDto,
    val hash: String,
    val releaseDate: String,
    val season: Int,
    val number: Int,
    val title: String?,
    val url: String,
    val image: String,
    val duration: Long,
) : Serializable