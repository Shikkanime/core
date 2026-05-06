package fr.shikkanime.services

import com.google.inject.Inject
import fr.shikkanime.dtos.GenericDto
import fr.shikkanime.entities.Anime
import fr.shikkanime.entities.Member
import fr.shikkanime.entities.MemberFollowAnime
import fr.shikkanime.repositories.MemberFollowAnimeRepository
import fr.shikkanime.utils.InvalidationService
import fr.shikkanime.utils.routes.Response
import java.util.*

class MemberFollowAnimeService : AbstractService<MemberFollowAnime, MemberFollowAnimeRepository>() {
    @Inject private lateinit var memberService: MemberService
    @Inject private lateinit var animeService: AnimeService

    suspend fun findAllFollowedAnimes(memberUuid: UUID, page: Int, limit: Int) =
        repository.findAllFollowedAnimes(memberUuid, page, limit)

    suspend fun findAllFollowedAnimesUUID(memberUuid: UUID) = repository.findAllFollowedAnimesUUID(memberUuid)

    suspend fun findAllByAnime(anime: Anime) = repository.findAllByAnime(anime)

    suspend fun findAllMissedAnimes(memberUuid: UUID, page: Int, limit: Int) =
        repository.findAllMissedAnimes(memberUuid, page, limit)

    suspend fun findAllByMember(memberUuid: UUID) = repository.findAllByMember(memberUuid)

    suspend fun existsByMemberUuidAndAnimeUuid(memberUuid: UUID, animeUuid: UUID) =
        repository.existsByMemberUuidAndAnimeUuid(memberUuid, animeUuid)

    suspend fun existsByMemberAndAnime(member: Member, anime: Anime) = existsByMemberUuidAndAnimeUuid(member.uuid!!, anime.uuid!!)

    suspend fun deleteAllByMember(memberUuid: UUID) = repository.deleteAll(repository.findAllByMember(memberUuid))

    suspend fun follow(memberUuid: UUID, anime: GenericDto): Response {
        val animeReference = animeService.getReference(anime.uuid) ?: return Response.notFound()
        if (repository.existsByMemberUuidAndAnimeUuid(memberUuid, animeReference.uuid!!))
            return Response.conflict()

        save(MemberFollowAnime(member = memberService.getReference(memberUuid), anime = animeReference))
        InvalidationService.invalidate(MemberFollowAnime::class.java)
        return Response.ok()
    }

    suspend fun unfollow(memberUuid: UUID, anime: GenericDto): Response {
        val memberFollowAnime = repository.findByMemberUuidAndAnimeUuid(memberUuid, anime.uuid)
            ?: return Response.conflict()

        repository.delete(memberFollowAnime)
        InvalidationService.invalidate(MemberFollowAnime::class.java)
        return Response.ok()
    }
}