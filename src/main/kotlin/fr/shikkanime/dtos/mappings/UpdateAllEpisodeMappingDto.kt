package fr.shikkanime.dtos.mappings

import fr.shikkanime.entities.enums.EpisodeType
import java.util.UUID

data class UpdateAllEpisodeMappingDto(
    val uuids: Set<UUID>,
    val episodeType: EpisodeType?,
    val season: Int?,
    val forceUpdate: Boolean?
)