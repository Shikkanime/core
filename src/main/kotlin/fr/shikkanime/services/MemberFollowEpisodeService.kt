package fr.shikkanime.services

import com.google.inject.Inject
import fr.shikkanime.dtos.AllFollowedEpisodeDto
import fr.shikkanime.dtos.GenericDto
import fr.shikkanime.entities.EpisodeMapping
import fr.shikkanime.entities.Member
import fr.shikkanime.entities.MemberFollowEpisode
import fr.shikkanime.entities.TraceAction
import fr.shikkanime.entities.enums.EpisodeType
import fr.shikkanime.repositories.MemberFollowEpisodeRepository
import fr.shikkanime.services.caches.MemberCacheService
import fr.shikkanime.utils.MapCache
import fr.shikkanime.utils.routes.Response
import java.time.ZonedDateTime
import java.util.*

class MemberFollowEpisodeService : AbstractService<MemberFollowEpisode, MemberFollowEpisodeRepository>() {
    @Inject private lateinit var memberFollowEpisodeRepository: MemberFollowEpisodeRepository
    @Inject private lateinit var memberCacheService: MemberCacheService
    @Inject private lateinit var episodeMappingService: EpisodeMappingService
    @Inject private lateinit var animeService: AnimeService
    @Inject private lateinit var traceActionService: TraceActionService

    override fun getRepository() = memberFollowEpisodeRepository

    fun findAllFollowedEpisodes(member: Member, page: Int, limit: Int) = memberFollowEpisodeRepository.findAllFollowedEpisodes(member, page, limit)

    fun findAllFollowedEpisodesUUID(memberUuid: UUID) = memberFollowEpisodeRepository.findAllFollowedEpisodesUUID(memberUuid)

    fun findAllByEpisode(episodeMapping: EpisodeMapping) =
        memberFollowEpisodeRepository.findAllByEpisode(episodeMapping)

    fun getSeenAndUnseenDuration(member: Member) = memberFollowEpisodeRepository.getSeenAndUnseenDuration(member)

    fun existsByMemberAndEpisode(memberUuid: UUID, episodeUuid: UUID) =
        memberFollowEpisodeRepository.existsByMemberAndEpisode(memberUuid, episodeUuid)

    fun followAll(memberUuid: UUID, anime: GenericDto): Response {
        val member = memberCacheService.find(memberUuid) ?: return Response.notFound()
        val elements = episodeMappingService.findAllByAnime(animeService.find(anime.uuid) ?: return Response.notFound())
            .filter { it.episodeType != EpisodeType.SUMMARY }
        val followed = memberFollowEpisodeRepository.findAllFollowedEpisodesByMemberAndEpisodes(member, elements)
        val now = ZonedDateTime.now()

        val filtered = elements.filter { it.uuid !in followed }.map { MemberFollowEpisode(followDateTime = now, member = member, episode = it) }
        memberFollowEpisodeRepository.saveAll(filtered)
        filtered.forEach { traceActionService.createTraceAction(it, TraceAction.Action.CREATE) }
        MapCache.invalidate(MemberFollowEpisode::class.java)

        return Response.ok(AllFollowedEpisodeDto(data = filtered.mapNotNull { it.episode?.uuid }.toSet(), duration = filtered.sumOf { it.episode!!.duration }))
    }

    fun follow(memberUuid: UUID, episode: GenericDto): Response {
        val member = memberCacheService.find(memberUuid) ?: return Response.notFound()
        val element = episodeMappingService.find(episode.uuid) ?: return Response.notFound()

        if (existsByMemberAndEpisode(memberUuid, episode.uuid)) {
            return Response.conflict()
        }

        val saved = save(MemberFollowEpisode(member = member, episode = element))
        traceActionService.createTraceAction(saved, TraceAction.Action.CREATE)
        MapCache.invalidate(MemberFollowEpisode::class.java)
        return Response.ok()
    }

    fun unfollow(memberUuid: UUID, episode: GenericDto): Response {
        val member = memberCacheService.find(memberUuid) ?: return Response.notFound()
        val element = episodeMappingService.find(episode.uuid) ?: return Response.notFound()

        val findByMemberAndEpisode = memberFollowEpisodeRepository.findByMemberAndEpisode(member, element)
            ?: return Response.conflict()

        memberFollowEpisodeRepository.delete(findByMemberAndEpisode)
        traceActionService.createTraceAction(findByMemberAndEpisode, TraceAction.Action.DELETE)
        MapCache.invalidate(MemberFollowEpisode::class.java)
        return Response.ok()
    }
}