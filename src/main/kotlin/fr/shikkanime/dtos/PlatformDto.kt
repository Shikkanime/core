package fr.shikkanime.dtos

import java.io.Serializable

data class PlatformDto(
    val id: String,
    val name: String,
    val url: String,
    val image: String
) : Serializable, Comparable<PlatformDto> {
    override fun compareTo(other: PlatformDto): Int {
        return name.compareTo(other.name)
    }
}
