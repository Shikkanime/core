package fr.shikkanime.services

import com.google.inject.Inject
import fr.shikkanime.dtos.AllFollowedEpisodeDto
import fr.shikkanime.dtos.GenericDto
import fr.shikkanime.entities.EpisodeMapping
import fr.shikkanime.entities.Member
import fr.shikkanime.entities.MemberFollowEpisode
import fr.shikkanime.entities.TraceAction
import fr.shikkanime.repositories.MemberFollowEpisodeRepository
import fr.shikkanime.utils.InvalidationService
import fr.shikkanime.utils.routes.Response
import fr.shikkanime.utils.takeIfNotEmpty
import java.time.ZonedDateTime
import java.util.*

class MemberFollowEpisodeService : AbstractService<MemberFollowEpisode, MemberFollowEpisodeRepository>() {
    @Inject private lateinit var memberFollowEpisodeRepository: MemberFollowEpisodeRepository

    @Inject
    private lateinit var memberService: MemberService
    @Inject private lateinit var episodeMappingService: EpisodeMappingService
    @Inject private lateinit var traceActionService: TraceActionService

    override fun getRepository() = memberFollowEpisodeRepository

    fun findAllFollowedEpisodes(memberUuid: UUID, page: Int, limit: Int) = memberFollowEpisodeRepository.findAllFollowedEpisodes(memberUuid, page, limit)

    fun findAllFollowedEpisodesUUID(memberUuid: UUID) = memberFollowEpisodeRepository.findAllFollowedEpisodesUUID(memberUuid)

    fun findAllByEpisode(episodeMapping: EpisodeMapping) =
        memberFollowEpisodeRepository.findAllByEpisode(episodeMapping)

    fun getSeenAndUnseenDuration(member: Member) = memberFollowEpisodeRepository.getSeenAndUnseenDuration(member)

    fun followAll(memberUuid: UUID, anime: GenericDto): Response {
        val nonFollowedEpisodes = memberFollowEpisodeRepository
            .findAllNonFollowedEpisodesByMemberUuidAndAnimeUuid(memberUuid, anime.uuid)
            .takeIfNotEmpty() ?: return Response.ok(AllFollowedEpisodeDto(emptySet(), 0))

        val now = ZonedDateTime.now()
        val memberFollowEpisodes = nonFollowedEpisodes.map { episode ->
            MemberFollowEpisode(followDateTime = now, member = memberService.getReference(memberUuid), episode = episode)
        }

        saveAll(memberFollowEpisodes)
        traceActionService.createTraceActions(memberFollowEpisodes, TraceAction.Action.CREATE)
        InvalidationService.invalidate(MemberFollowEpisode::class.java)

        val episodeUuids = nonFollowedEpisodes.mapNotNull { it.uuid }.toSet()
        val totalDuration = nonFollowedEpisodes.sumOf { it.duration }

        return Response.ok(AllFollowedEpisodeDto(data = episodeUuids, duration = totalDuration))
    }

    fun follow(memberUuid: UUID, episode: GenericDto): Response {
        val episodeReference = episodeMappingService.getReference(episode.uuid) ?: return Response.notFound()
        if (memberFollowEpisodeRepository.existsByMemberUuidAndEpisodeUuid(memberUuid, episodeReference.uuid!!))
            return Response.conflict()

        val memberFollowEpisode =
            save(MemberFollowEpisode(member = memberService.getReference(memberUuid), episode = episodeReference))
        traceActionService.createTraceAction(memberFollowEpisode, TraceAction.Action.CREATE)
        InvalidationService.invalidate(MemberFollowEpisode::class.java)
        return Response.ok()
    }

    fun unfollow(memberUuid: UUID, episode: GenericDto): Response {
        val memberFollowEpisode = memberFollowEpisodeRepository.findByMemberUuidAndEpisodeUuid(memberUuid, episode.uuid)
            ?: return Response.conflict()

        memberFollowEpisodeRepository.delete(memberFollowEpisode)
        traceActionService.createTraceAction(memberFollowEpisode, TraceAction.Action.DELETE)
        InvalidationService.invalidate(MemberFollowEpisode::class.java)
        return Response.ok()
    }
}