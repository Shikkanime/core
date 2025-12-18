package fr.shikkanime.services

import com.google.inject.Inject
import fr.shikkanime.dtos.GenericDto
import fr.shikkanime.entities.Anime
import fr.shikkanime.entities.Member
import fr.shikkanime.entities.MemberFollowAnime
import fr.shikkanime.entities.TraceAction
import fr.shikkanime.repositories.MemberFollowAnimeRepository
import fr.shikkanime.utils.InvalidationService
import fr.shikkanime.utils.routes.Response
import java.util.*

class MemberFollowAnimeService : AbstractService<MemberFollowAnime, MemberFollowAnimeRepository>() {
    @Inject private lateinit var memberFollowAnimeRepository: MemberFollowAnimeRepository

    @Inject
    private lateinit var memberService: MemberService
    @Inject private lateinit var animeService: AnimeService
    @Inject private lateinit var traceActionService: TraceActionService

    override fun getRepository() = memberFollowAnimeRepository

    fun findAllFollowedAnimes(memberUuid: UUID, page: Int, limit: Int) = memberFollowAnimeRepository.findAllFollowedAnimes(memberUuid, page, limit)

    fun findAllFollowedAnimesUUID(memberUuid: UUID) = memberFollowAnimeRepository.findAllFollowedAnimesUUID(memberUuid)

    fun findAllByAnime(anime: Anime) = memberFollowAnimeRepository.findAllByAnime(anime)

    fun findAllMissedAnimes(memberUuid: UUID, page: Int, limit: Int) =
        memberFollowAnimeRepository.findAllMissedAnimes(memberUuid, page, limit)

    fun existsByMemberUuidAndAnimeUuid(memberUuid: UUID, animeUuid: UUID) = memberFollowAnimeRepository.existsByMemberUuidAndAnimeUuid(memberUuid, animeUuid)

    fun existsByMemberAndAnime(member: Member, anime: Anime) = existsByMemberUuidAndAnimeUuid(member.uuid!!, anime.uuid!!)

    fun follow(memberUuid: UUID, anime: GenericDto): Response {
        val animeReference = animeService.getReference(anime.uuid) ?: return Response.notFound()
        if (memberFollowAnimeRepository.existsByMemberUuidAndAnimeUuid(memberUuid, animeReference.uuid!!))
            return Response.conflict()

        val memberFollowAnime = save(MemberFollowAnime(member = memberService.getReference(memberUuid), anime = animeReference))
        traceActionService.createTraceAction(memberFollowAnime, TraceAction.Action.CREATE)
        InvalidationService.invalidate(MemberFollowAnime::class.java)
        return Response.ok()
    }

    fun unfollow(memberUuid: UUID, anime: GenericDto): Response {
        val memberFollowAnime = memberFollowAnimeRepository.findByMemberUuidAndAnimeUuid(memberUuid, anime.uuid)
            ?: return Response.conflict()

        memberFollowAnimeRepository.delete(memberFollowAnime)
        traceActionService.createTraceAction(memberFollowAnime, TraceAction.Action.DELETE)
        InvalidationService.invalidate(MemberFollowAnime::class.java)
        return Response.ok()
    }
}