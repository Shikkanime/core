package fr.shikkanime.dtos.member

import java.util.*

data class MemberDto(
    val uuid: UUID,
    val token: String,
    val creationDateTime: String,
    val lastUpdateDateTime: String,
    val isPrivate: Boolean,
    val email: String?,
    val followedAnimes: Set<UUID>,
    val followedEpisodes: Set<UUID>,
    val totalDuration: Long,
    val totalUnseenDuration: Long,
    val hasProfilePicture: Boolean = false,
    val attachmentLastUpdateDateTime: String? = null,
)
