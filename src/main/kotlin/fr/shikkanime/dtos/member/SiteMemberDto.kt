package fr.shikkanime.dtos.member

import java.util.UUID

data class SiteMemberDto(
    val uuid: UUID,
    val creationDateTime: String,
    val username: String?,
    val totalDuration: Long,
    val totalUnseenDuration: Long,
    val hasProfilePicture: Boolean = false,
)
