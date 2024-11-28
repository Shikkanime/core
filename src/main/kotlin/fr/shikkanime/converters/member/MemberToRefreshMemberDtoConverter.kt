package fr.shikkanime.converters.member

import com.google.inject.Inject
import fr.shikkanime.converters.AbstractConverter
import fr.shikkanime.dtos.animes.AnimeDto
import fr.shikkanime.dtos.animes.MissedAnimeDto
import fr.shikkanime.dtos.PageableDto
import fr.shikkanime.dtos.mappings.EpisodeMappingDto
import fr.shikkanime.dtos.member.RefreshMemberDto
import fr.shikkanime.entities.Member
import fr.shikkanime.services.MemberFollowAnimeService
import fr.shikkanime.services.MemberFollowEpisodeService

class MemberToRefreshMemberDtoConverter : AbstractConverter<Member, RefreshMemberDto>() {
    @Inject
    private lateinit var memberFollowAnimeService: MemberFollowAnimeService

    @Inject
    private lateinit var memberFollowEpisodeService: MemberFollowEpisodeService

    @Converter
    fun convert(from: Member, limit: Int): RefreshMemberDto {
        val missedAnimesPageable = memberFollowAnimeService.findAllMissedAnimes(from, 1, limit)
        val followedAnimesPageable = memberFollowAnimeService.findAllFollowedAnimes(from, 1, limit)
        val followedEpisodesPageable = memberFollowEpisodeService.findAllFollowedEpisodes(from, 1, limit)
        val (totalDuration, totalUnseenDuration) = memberFollowEpisodeService.getSeenAndUnseenDuration(from)

        val missedAnimeDtos = missedAnimesPageable.data.map { tuple ->
            MissedAnimeDto(
                convert(tuple[0], AnimeDto::class.java),
                tuple[1] as Long
            )
        }

        return RefreshMemberDto(
            missedAnimes = PageableDto(
                data = missedAnimeDtos,
                page = missedAnimesPageable.page,
                limit = missedAnimesPageable.limit,
                total = missedAnimesPageable.total,
            ),
            followedAnimes = PageableDto.fromPageable(followedAnimesPageable, AnimeDto::class.java),
            followedEpisodes = PageableDto.fromPageable(followedEpisodesPageable, EpisodeMappingDto::class.java),
            totalDuration = totalDuration,
            totalUnseenDuration = totalUnseenDuration,
        )
    }
}