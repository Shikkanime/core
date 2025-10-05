package fr.shikkanime.dtos

import fr.shikkanime.entities.enums.LangType
import java.io.Serializable

data class EpisodeSourceDto(
    val platform: PlatformDto,
    val url: String,
    val langType: LangType
) : Serializable, Comparable<EpisodeSourceDto> {
    override fun compareTo(other: EpisodeSourceDto): Int {
        return compareBy<EpisodeSourceDto>({ it.langType }, { it.platform.name }).compare(this, other)
    }
}
