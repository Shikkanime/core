package fr.shikkanime.dtos

data class LoginDto(
    val identifier: String,
    val appVersion: String,
    val device: String,
    val locale: String,
)
