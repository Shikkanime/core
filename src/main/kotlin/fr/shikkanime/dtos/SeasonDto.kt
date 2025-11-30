package fr.shikkanime.dtos

import java.io.Serializable

data class SeasonDto(
    val number: Int,
    val releaseDateTime: String,
    val lastReleaseDateTime: String,
    val episodes: Long
) : Serializable
