package fr.shikkanime.dtos.member

import fr.shikkanime.dtos.PageableDto
import fr.shikkanime.dtos.animes.AnimeDto
import fr.shikkanime.dtos.mappings.EpisodeMappingDto
import java.util.UUID

data class SiteMemberDto(
    val uuid: UUID,
    val creationDateTime: String,
    val username: String?,
    val totalDuration: Long,
    val totalUnseenDuration: Long,
    val hasProfilePicture: Boolean = false,
    val followedAnimesDto: PageableDto<AnimeDto>,
    val followedEpisodesDto: PageableDto<EpisodeMappingDto>
)
