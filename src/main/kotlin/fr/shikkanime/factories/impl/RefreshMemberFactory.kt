package fr.shikkanime.factories.impl

import com.google.inject.Inject
import fr.shikkanime.dtos.member.RefreshMemberDto
import fr.shikkanime.entities.Member
import fr.shikkanime.factories.IRefreshMemberFactory
import fr.shikkanime.services.MemberFollowEpisodeService
import fr.shikkanime.services.caches.MemberFollowAnimeCacheService
import fr.shikkanime.services.caches.MemberFollowEpisodeCacheService

class RefreshMemberFactory : IRefreshMemberFactory {
    @Inject private lateinit var memberFollowAnimeCacheService: MemberFollowAnimeCacheService
    @Inject private lateinit var memberFollowEpisodeCacheService: MemberFollowEpisodeCacheService
    @Inject private lateinit var memberFollowEpisodeService: MemberFollowEpisodeService

    override fun toDto(entity: Member, limit: Int): RefreshMemberDto {
        val memberUuid = entity.uuid!!
        val missedAnimesPageable = memberFollowAnimeCacheService.getMissedAnimes(memberUuid, 1, limit)
        val followedAnimesPageable = memberFollowAnimeCacheService.findAllBy(memberUuid, 1, limit)
        val followedEpisodesPageable = memberFollowEpisodeCacheService.findAllBy(memberUuid, 1, limit)
        val (totalDuration, totalUnseenDuration) = memberFollowEpisodeService.getSeenAndUnseenDuration(entity)

        return RefreshMemberDto(
            missedAnimes = missedAnimesPageable,
            followedAnimes = followedAnimesPageable,
            followedEpisodes = followedEpisodesPageable,
            totalDuration = totalDuration,
            totalUnseenDuration = totalUnseenDuration,
        )
    }
}