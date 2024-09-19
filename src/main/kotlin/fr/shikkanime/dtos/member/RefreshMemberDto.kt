package fr.shikkanime.dtos.member

import fr.shikkanime.dtos.AnimeDto
import fr.shikkanime.dtos.MissedAnimeDto
import fr.shikkanime.dtos.PageableDto
import fr.shikkanime.dtos.mappings.EpisodeMappingDto

data class RefreshMemberDto(
    val missedAnimes: PageableDto<MissedAnimeDto>,
    val followedAnimes: PageableDto<AnimeDto>,
    val followedEpisodes: PageableDto<EpisodeMappingDto>,
    val totalDuration: Long,
    val totalUnseenDuration: Long,
)
