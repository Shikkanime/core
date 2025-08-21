package fr.shikkanime.dtos

import java.io.Serializable

data class SeasonDto(
    val number: Int,
    val lastReleaseDateTime: String,
) : Serializable
