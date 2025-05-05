package fr.shikkanime.dtos

@Deprecated("Use the new /v1/members/login endpoint")
data class LoginDto(
    val identifier: String,
    val appVersion: String,
    val device: String,
    val locale: String,
)
