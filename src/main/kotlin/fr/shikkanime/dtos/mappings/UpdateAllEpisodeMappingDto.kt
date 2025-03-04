package fr.shikkanime.dtos.mappings

import fr.shikkanime.entities.enums.EpisodeType
import java.util.*

data class UpdateAllEpisodeMappingDto(
    val uuids: Set<UUID>,
    val episodeType: EpisodeType?,
    val season: Int?,
    val startDate: String?,
    val incrementDate: Boolean?,
    val bindVoiceVariants: Boolean?,
    val forceUpdate: Boolean?,
    val bindNumber: Boolean?,
)