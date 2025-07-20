package fr.shikkanime.dtos

data class LoginCountDto(
    val date: String,
    val distinctCount: Long,
    val count: Long
)
