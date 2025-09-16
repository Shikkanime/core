package fr.shikkanime.dtos.mappings

import fr.shikkanime.dtos.EpisodeSourceDto
import fr.shikkanime.dtos.PlatformDto
import fr.shikkanime.dtos.animes.AnimeDto
import fr.shikkanime.entities.enums.EpisodeType
import fr.shikkanime.entities.enums.LangType
import java.io.Serializable
import java.util.*

data class GroupedEpisodeDto(
    val anime: AnimeDto,
    @Deprecated("Use sources instead") val platforms: Set<PlatformDto>,
    val releaseDateTime: String,
    val lastUpdateDateTime: String,
    val season: String,
    val episodeType: EpisodeType,
    val number: String,
    @Deprecated("Use sources instead") val langTypes: Set<LangType>,
    val title: String?,
    val description: String?,
    val duration: Long?,
    val internalUrl: String?,
    val mappings: Set<UUID>,
    @Deprecated("Use sources instead") val urls: Set<String>,
    val sources: Set<EpisodeSourceDto>
) : Serializable