package fr.shikkanime.dtos.mappings

import fr.shikkanime.dtos.PlatformDto
import fr.shikkanime.dtos.UUIDAndNameDto
import fr.shikkanime.entities.enums.CountryCode
import fr.shikkanime.entities.enums.EpisodeType
import fr.shikkanime.entities.enums.LangType
import java.util.*

data class GroupedEpisodeDto(
    val countryCode: CountryCode,
    val anime: UUIDAndNameDto,
    val platforms: Set<PlatformDto>,
    val releaseDateTime: String,
    val season: String,
    val episodeType: EpisodeType,
    val number: String,
    val langTypes: Set<LangType>,
    val title: String?,
    val description: String?,
    val duration: Long?,
    val internalUrl: String?,
    val mappings: Set<UUID>,
    val urls: Set<String>
)