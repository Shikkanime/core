package fr.shikkanime.dtos

import fr.shikkanime.entities.enums.EpisodeType
import java.util.*

data class UpdateAllEpisodeMappingDto(
    val uuids: List<UUID>,
    val episodeType: EpisodeType?,
    val season: Int?,
)
