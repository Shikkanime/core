package fr.shikkanime.services

import com.google.inject.Inject
import fr.shikkanime.dtos.AllFollowedEpisodeDto
import fr.shikkanime.dtos.GenericDto
import fr.shikkanime.entities.Member
import fr.shikkanime.entities.MemberFollowEpisode
import fr.shikkanime.repositories.MemberFollowEpisodeRepository
import fr.shikkanime.utils.MapCache
import fr.shikkanime.utils.routes.Response
import java.time.ZonedDateTime
import java.util.*

class MemberFollowEpisodeService : AbstractService<MemberFollowEpisode, MemberFollowEpisodeRepository>() {
    @Inject
    private lateinit var memberFollowEpisodeRepository: MemberFollowEpisodeRepository

    @Inject
    private lateinit var memberService: MemberService

    @Inject
    private lateinit var episodeMappingService: EpisodeMappingService

    @Inject
    private lateinit var animeService: AnimeService

    override fun getRepository() = memberFollowEpisodeRepository

    fun findAllFollowedEpisodesUUID(member: Member) = memberFollowEpisodeRepository.findAllFollowedEpisodesUUID(member)

    fun findAllFollowedEpisodes(member: Member) = memberFollowEpisodeRepository.findAllFollowedEpisodes(member)

    fun getTotalDuration(member: Member) = memberFollowEpisodeRepository.getTotalDuration(member)

    fun followAll(uuidUser: UUID, anime: GenericDto): Response {
        val member = memberService.find(uuidUser) ?: return Response.notFound()
        val elements = episodeMappingService.findAllByAnime(animeService.find(anime.uuid) ?: return Response.notFound())
        val list = mutableListOf<MemberFollowEpisode>()

        elements.forEach { element ->
            if (memberFollowEpisodeRepository.findByMemberAndEpisode(member, element) != null) {
                return@forEach
            }

            list.add(save(MemberFollowEpisode(member = member, episode = element)))
        }

        member.lastUpdateDateTime = ZonedDateTime.now()
        memberService.update(member)
        MapCache.invalidate(MemberFollowEpisode::class.java)

        return Response.ok(AllFollowedEpisodeDto(data = list.mapNotNull { it.episode?.uuid }.toSet(), duration = list.sumOf { it.episode!!.duration }))
    }

    fun follow(uuidUser: UUID, episode: GenericDto): Response {
        val member = memberService.find(uuidUser) ?: return Response.notFound()
        val element = episodeMappingService.find(episode.uuid) ?: return Response.notFound()

        if (memberFollowEpisodeRepository.findByMemberAndEpisode(member, element) != null) {
            return Response.conflict()
        }

        member.lastUpdateDateTime = ZonedDateTime.now()
        memberService.update(member)
        save(MemberFollowEpisode(member = member, episode = element))
        MapCache.invalidate(MemberFollowEpisode::class.java)
        return Response.ok()
    }

    fun unfollow(uuidUser: UUID, episode: GenericDto): Response {
        val member = memberService.find(uuidUser) ?: return Response.notFound()
        val element = episodeMappingService.find(episode.uuid) ?: return Response.notFound()

        val findByMemberAndEpisode = memberFollowEpisodeRepository.findByMemberAndEpisode(member, element)
            ?: return Response.conflict()

        member.lastUpdateDateTime = ZonedDateTime.now()
        memberService.update(member)
        memberFollowEpisodeRepository.delete(findByMemberAndEpisode)
        MapCache.invalidate(MemberFollowEpisode::class.java)
        return Response.ok()
    }
}