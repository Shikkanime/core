package fr.shikkanime.dtos

import java.io.Serializable
import java.util.*

data class AnimePlatformDto(
    val uuid: UUID?,
    val platform: PlatformDto,
    val platformId: String,
    val lastValidateDateTime: String?,
) : Serializable, Comparable<AnimePlatformDto> {
    override fun compareTo(other: AnimePlatformDto): Int {
        return platform.name.compareTo(other.platform.name)
    }
}
