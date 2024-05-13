package fr.shikkanime.services

import com.google.inject.Inject
import fr.shikkanime.dtos.GenericDto
import fr.shikkanime.entities.EpisodeMapping
import fr.shikkanime.entities.Member
import fr.shikkanime.entities.MemberFollowAnime
import fr.shikkanime.repositories.MemberFollowAnimeRepository
import fr.shikkanime.utils.MapCache
import fr.shikkanime.utils.routes.Response
import java.time.ZonedDateTime
import java.util.*

class MemberFollowAnimeService : AbstractService<MemberFollowAnime, MemberFollowAnimeRepository>() {
    @Inject
    private lateinit var memberFollowAnimeRepository: MemberFollowAnimeRepository

    @Inject
    private lateinit var memberService: MemberService

    @Inject
    private lateinit var animeService: AnimeService

    override fun getRepository() = memberFollowAnimeRepository

    fun findAllFollowedAnimesUUID(member: Member) = memberFollowAnimeRepository.findAllFollowedAnimesUUID(member)

    fun findAllMissedAnimes(member: Member, watchedEpisodes: List<EpisodeMapping>, page: Int, limit: Int) =
        memberFollowAnimeRepository.findAllMissedAnimes(member, watchedEpisodes, page, limit)

    fun follow(uuidUser: UUID, anime: GenericDto): Response {
        val member = memberService.find(uuidUser) ?: return Response.notFound()
        val element = animeService.find(anime.uuid) ?: return Response.notFound()

        if (memberFollowAnimeRepository.findByMemberAndAnime(member, element) != null) {
            return Response.conflict()
        }

        member.lastUpdateDateTime = ZonedDateTime.now()
        memberService.update(member)
        save(MemberFollowAnime(member = member, anime = element))
        MapCache.invalidate(MemberFollowAnime::class.java)
        return Response.ok()
    }

    fun unfollow(uuidUser: UUID, anime: GenericDto): Response {
        val member = memberService.find(uuidUser) ?: return Response.notFound()
        val element = animeService.find(anime.uuid) ?: return Response.notFound()

        val findByMemberAndAnime = memberFollowAnimeRepository.findByMemberAndAnime(member, element)
            ?: return Response.conflict()

        member.lastUpdateDateTime = ZonedDateTime.now()
        memberService.update(member)
        memberFollowAnimeRepository.delete(findByMemberAndAnime)
        MapCache.invalidate(MemberFollowAnime::class.java)
        return Response.ok()
    }
}