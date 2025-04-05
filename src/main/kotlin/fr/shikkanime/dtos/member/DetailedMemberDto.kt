package fr.shikkanime.dtos.member

import java.util.*

data class DetailedMemberDto(
    val uuid: UUID,
    val email: String?,
    val creationDateTime: String,
    val lastUpdateDateTime: String?,
    val lastLoginDateTime: String?,
    val followedAnimesCount: Long,
    val followedEpisodesCount: Long,
    var hasProfilePicture: Boolean = false,
    var isActive: Boolean = false,
    var additionalData: AdditionalDataDto? = null,
)

data class AdditionalDataDto(
    val appVersion: String,
    val device: String,
    val locale: String,
)