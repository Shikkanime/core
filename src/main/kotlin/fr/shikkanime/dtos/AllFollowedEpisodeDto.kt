package fr.shikkanime.dtos

import java.util.*

data class AllFollowedEpisodeDto(
    val data: Set<UUID>,
    val duration: Long,
)
