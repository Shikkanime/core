package fr.shikkanime.dtos

import java.util.*

data class MemberDto(
    val uuid: UUID,
    val token: String,
    val creationDateTime: String,
    val lastUpdateDateTime: String,
    val isPrivate: Boolean,
    val email: String?,
    val followedAnimes: List<UUID>,
    val followedEpisodes: List<UUID>,
    val totalDuration: Long,
)
