package fr.shikkanime.services

import com.google.inject.Inject
import fr.shikkanime.dtos.GenericDto
import fr.shikkanime.entities.Anime
import fr.shikkanime.entities.Member
import fr.shikkanime.entities.MemberFollowAnime
import fr.shikkanime.entities.TraceAction
import fr.shikkanime.repositories.MemberFollowAnimeRepository
import fr.shikkanime.services.caches.MemberCacheService
import fr.shikkanime.utils.MapCache
import fr.shikkanime.utils.TelemetryConfig
import fr.shikkanime.utils.TelemetryConfig.trace
import fr.shikkanime.utils.routes.Response
import java.util.*

class MemberFollowAnimeService : AbstractService<MemberFollowAnime, MemberFollowAnimeRepository>() {
    private val tracer = TelemetryConfig.getTracer("MemberFollowAnimeService")
    @Inject private lateinit var memberFollowAnimeRepository: MemberFollowAnimeRepository
    @Inject private lateinit var memberCacheService: MemberCacheService
    @Inject private lateinit var animeService: AnimeService
    @Inject private lateinit var traceActionService: TraceActionService

    override fun getRepository() = memberFollowAnimeRepository

    fun findAllFollowedAnimes(member: Member, page: Int, limit: Int) = tracer.trace { memberFollowAnimeRepository.findAllFollowedAnimes(member, page, limit) }

    fun findAllFollowedAnimesUUID(memberUuid: UUID) = tracer.trace { memberFollowAnimeRepository.findAllFollowedAnimesUUID(memberUuid) }

    fun findAllByAnime(anime: Anime) = memberFollowAnimeRepository.findAllByAnime(anime)

    fun findAllMissedAnimes(member: Member, page: Int, limit: Int) =
        tracer.trace { memberFollowAnimeRepository.findAllMissedAnimes(member, page, limit) }

    fun existsByMemberAndAnime(member: Member, anime: Anime) = tracer.trace { memberFollowAnimeRepository.existsByMemberAndAnime(member, anime) }

    fun findByMemberAndAnime(member: Member, anime: Anime) = tracer.trace { memberFollowAnimeRepository.findByMemberAndAnime(member, anime) }

    fun follow(memberUuid: UUID, anime: GenericDto): Response {
        val member = memberCacheService.find(memberUuid) ?: return Response.notFound()
        val element = animeService.find(anime.uuid) ?: return Response.notFound()

        if (existsByMemberAndAnime(member, element)) {
            return Response.conflict()
        }

        val saved = save(MemberFollowAnime(member = member, anime = element))
        traceActionService.createTraceAction(saved, TraceAction.Action.CREATE)
        MapCache.invalidate(MemberFollowAnime::class.java)
        return Response.ok()
    }

    fun unfollow(memberUuid: UUID, anime: GenericDto): Response {
        val member = memberCacheService.find(memberUuid) ?: return Response.notFound()
        val element = animeService.find(anime.uuid) ?: return Response.notFound()

        val findByMemberAndAnime = findByMemberAndAnime(member, element)
            ?: return Response.conflict()

        memberFollowAnimeRepository.delete(findByMemberAndAnime)
        traceActionService.createTraceAction(findByMemberAndAnime, TraceAction.Action.DELETE)
        MapCache.invalidate(MemberFollowAnime::class.java)
        return Response.ok()
    }
}