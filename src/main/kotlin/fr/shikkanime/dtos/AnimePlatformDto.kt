package fr.shikkanime.dtos

import java.io.Serializable
import java.util.*
import kotlin.comparisons.compareValuesBy

data class AnimePlatformDto(
    val uuid: UUID?,
    val platform: PlatformDto,
    val platformId: String,
    val lastValidateDateTime: String?,
) : Serializable, Comparable<AnimePlatformDto> {
    override fun compareTo(other: AnimePlatformDto): Int {
        return compareValuesBy(
            this,
            other,
            { it.platform.name },
            { it.platformId },
            { it.uuid }
        )
    }
}
