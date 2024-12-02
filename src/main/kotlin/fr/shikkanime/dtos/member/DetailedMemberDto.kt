package fr.shikkanime.dtos.member

import java.util.*

data class DetailedMemberDto(
    val uuid: UUID,
    val email: String?,
    val creationDateTime: String,
    val lastUpdateDateTime: String,
    val lastLoginDateTime: String?,
    val followedAnimesCount: Long,
    val followedEpisodesCount: Long,
    var hasProfilePicture: Boolean = false,
)
