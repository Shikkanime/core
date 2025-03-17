package fr.shikkanime.factories.impl

import com.google.inject.Inject
import fr.shikkanime.dtos.PageableDto
import fr.shikkanime.dtos.member.RefreshMemberDto
import fr.shikkanime.entities.Member
import fr.shikkanime.factories.IRefreshMemberFactory
import fr.shikkanime.services.MemberFollowAnimeService
import fr.shikkanime.services.MemberFollowEpisodeService

class RefreshMemberFactory : IRefreshMemberFactory {
    @Inject
    private lateinit var memberFollowAnimeService: MemberFollowAnimeService

    @Inject
    private lateinit var memberFollowEpisodeService: MemberFollowEpisodeService

    @Inject
    private lateinit var missedAnimeFactory: MissedAnimeFactory

    @Inject
    private lateinit var animeFactory: AnimeFactory

    @Inject
    private lateinit var episodeMappingFactory: EpisodeMappingFactory

    override fun toDto(entity: Member, limit: Int): RefreshMemberDto {
        val missedAnimesPageable = memberFollowAnimeService.findAllMissedAnimes(entity, 1, limit)
        val followedAnimesPageable = memberFollowAnimeService.findAllFollowedAnimes(entity, 1, limit)
        val followedEpisodesPageable = memberFollowEpisodeService.findAllFollowedEpisodes(entity, 1, limit)
        val (totalDuration, totalUnseenDuration) = memberFollowEpisodeService.getSeenAndUnseenDuration(entity)

        return RefreshMemberDto(
            missedAnimes = PageableDto.fromPageable(missedAnimesPageable, missedAnimeFactory),
            followedAnimes = PageableDto.fromPageable(followedAnimesPageable, animeFactory),
            followedEpisodes = PageableDto.fromPageable(followedEpisodesPageable, episodeMappingFactory),
            totalDuration = totalDuration,
            totalUnseenDuration = totalUnseenDuration,
        )
    }
}