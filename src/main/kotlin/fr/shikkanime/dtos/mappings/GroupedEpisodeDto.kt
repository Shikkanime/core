package fr.shikkanime.dtos.mappings

import fr.shikkanime.dtos.EpisodeSourceDto
import fr.shikkanime.dtos.animes.AnimeDto
import fr.shikkanime.entities.enums.EpisodeType
import java.io.Serializable
import java.util.*

data class GroupedEpisodeDto(
    val anime: AnimeDto,
    val releaseDateTime: String,
    val lastUpdateDateTime: String,
    val season: String,
    val episodeType: EpisodeType,
    val number: String,
    val title: String?,
    val description: String?,
    val duration: Long?,
    val internalUrl: String?,
    val mappings: Set<UUID>,
    val sources: Set<EpisodeSourceDto>
) : Serializable