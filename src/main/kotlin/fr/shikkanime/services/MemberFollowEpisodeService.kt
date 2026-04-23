package fr.shikkanime.services

import com.google.inject.Inject
import fr.shikkanime.dtos.AllFollowedEpisodeDto
import fr.shikkanime.dtos.GenericDto
import fr.shikkanime.entities.EpisodeMapping
import fr.shikkanime.entities.Member
import fr.shikkanime.entities.MemberFollowEpisode
import fr.shikkanime.repositories.MemberFollowEpisodeRepository
import fr.shikkanime.utils.InvalidationService
import fr.shikkanime.utils.routes.Response
import fr.shikkanime.utils.takeIfNotEmpty
import java.time.ZonedDateTime
import java.util.*

class MemberFollowEpisodeService : AbstractService<MemberFollowEpisode, MemberFollowEpisodeRepository>() {
    @Inject private lateinit var memberService: MemberService
    @Inject private lateinit var episodeMappingService: EpisodeMappingService

    fun findAllFollowedEpisodes(memberUuid: UUID, page: Int, limit: Int) =
        repository.findAllFollowedEpisodes(memberUuid, page, limit)

    fun findAllFollowedEpisodesUUID(memberUuid: UUID) = repository.findAllFollowedEpisodesUUID(memberUuid)

    fun findAllByEpisode(episodeMapping: EpisodeMapping) =
        repository.findAllByEpisode(episodeMapping)

    fun findAllByMember(memberUuid: UUID) = repository.findAllByMember(memberUuid)

    fun getSeenAndUnseenDuration(member: Member) = repository.getSeenAndUnseenDuration(member)

    fun deleteAllByMember(memberUuid: UUID) = repository.deleteAll(repository.findAllByMember(memberUuid))

    fun followAll(memberUuid: UUID, anime: GenericDto): Response {
        val nonFollowedEpisodes = repository
            .findAllNonFollowedEpisodesByMemberUuidAndAnimeUuid(memberUuid, anime.uuid)
            .takeIfNotEmpty() ?: return Response.ok(AllFollowedEpisodeDto(emptySet(), 0))

        val now = ZonedDateTime.now()
        val memberFollowEpisodes = nonFollowedEpisodes.map { episode ->
            MemberFollowEpisode(followDateTime = now, member = memberService.getReference(memberUuid), episode = episode)
        }

        saveAll(memberFollowEpisodes)
        InvalidationService.invalidate(MemberFollowEpisode::class.java)

        val episodeUuids = nonFollowedEpisodes.mapNotNull { it.uuid }.toSet()
        val totalDuration = nonFollowedEpisodes.sumOf { it.duration }

        return Response.ok(AllFollowedEpisodeDto(data = episodeUuids, duration = totalDuration))
    }

    fun follow(memberUuid: UUID, episode: GenericDto): Response {
        val episodeReference = episodeMappingService.getReference(episode.uuid) ?: return Response.notFound()
        if (repository.existsByMemberUuidAndEpisodeUuid(memberUuid, episodeReference.uuid!!))
            return Response.conflict()

        save(MemberFollowEpisode(member = memberService.getReference(memberUuid), episode = episodeReference))
        InvalidationService.invalidate(MemberFollowEpisode::class.java)
        return Response.ok()
    }

    fun unfollow(memberUuid: UUID, episode: GenericDto): Response {
        val memberFollowEpisode = repository.findByMemberUuidAndEpisodeUuid(memberUuid, episode.uuid)
            ?: return Response.conflict()

        repository.delete(memberFollowEpisode)
        InvalidationService.invalidate(MemberFollowEpisode::class.java)
        return Response.ok()
    }
}