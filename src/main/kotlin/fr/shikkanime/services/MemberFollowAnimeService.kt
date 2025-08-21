package fr.shikkanime.services

import com.google.inject.Inject
import fr.shikkanime.dtos.GenericDto
import fr.shikkanime.entities.Anime
import fr.shikkanime.entities.Member
import fr.shikkanime.entities.MemberFollowAnime
import fr.shikkanime.entities.TraceAction
import fr.shikkanime.repositories.MemberFollowAnimeRepository
import fr.shikkanime.services.caches.MemberCacheService
import fr.shikkanime.utils.InvalidationService
import fr.shikkanime.utils.routes.Response
import java.util.*

class MemberFollowAnimeService : AbstractService<MemberFollowAnime, MemberFollowAnimeRepository>() {
    @Inject private lateinit var memberFollowAnimeRepository: MemberFollowAnimeRepository
    @Inject private lateinit var memberCacheService: MemberCacheService
    @Inject private lateinit var animeService: AnimeService
    @Inject private lateinit var traceActionService: TraceActionService

    override fun getRepository() = memberFollowAnimeRepository

    fun findAllFollowedAnimes(member: Member, page: Int, limit: Int) = memberFollowAnimeRepository.findAllFollowedAnimes(member, page, limit)

    fun findAllFollowedAnimesUUID(memberUuid: UUID) = memberFollowAnimeRepository.findAllFollowedAnimesUUID(memberUuid)

    fun findAllByAnime(anime: Anime) = memberFollowAnimeRepository.findAllByAnime(anime)

    fun findAllMissedAnimes(member: Member, page: Int, limit: Int) =
        memberFollowAnimeRepository.findAllMissedAnimes(member, page, limit)

    fun existsByMemberAndAnime(member: Member, anime: Anime) = memberFollowAnimeRepository.existsByMemberAndAnime(member, anime)

    fun follow(memberUuid: UUID, anime: GenericDto): Response {
        val member = memberCacheService.find(memberUuid) ?: return Response.notFound()
        val element = animeService.find(anime.uuid) ?: return Response.notFound()

        if (memberFollowAnimeRepository.existsByMemberAndAnime(member, element)) {
            return Response.conflict()
        }

        val saved = save(MemberFollowAnime(member = member, anime = element))
        traceActionService.createTraceAction(saved, TraceAction.Action.CREATE)
        InvalidationService.invalidate(MemberFollowAnime::class.java)
        return Response.ok()
    }

    fun unfollow(memberUuid: UUID, anime: GenericDto): Response {
        val member = memberCacheService.find(memberUuid) ?: return Response.notFound()
        val element = animeService.find(anime.uuid) ?: return Response.notFound()

        val findByMemberAndAnime = memberFollowAnimeRepository.findByMemberAndAnime(member, element)
            ?: return Response.conflict()

        memberFollowAnimeRepository.delete(findByMemberAndAnime)
        traceActionService.createTraceAction(findByMemberAndAnime, TraceAction.Action.DELETE)
        InvalidationService.invalidate(MemberFollowAnime::class.java)
        return Response.ok()
    }
}