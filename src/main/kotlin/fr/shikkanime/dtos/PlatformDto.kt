package fr.shikkanime.dtos

import java.io.Serializable

data class PlatformDto(
    val id: String,
    val name: String,
    val url: String,
    val image: String
) : Serializable
