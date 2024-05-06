package fr.shikkanime.converters.member

import com.google.inject.Inject
import fr.shikkanime.converters.AbstractConverter
import fr.shikkanime.dtos.MemberDto
import fr.shikkanime.dtos.TokenDto
import fr.shikkanime.entities.Member
import fr.shikkanime.services.MemberFollowAnimeService
import fr.shikkanime.services.MemberFollowEpisodeService
import fr.shikkanime.utils.withUTCString

class MemberToMemberDtoConverter : AbstractConverter<Member, MemberDto>() {
    @Inject
    private lateinit var memberFollowAnimeService: MemberFollowAnimeService

    @Inject
    private lateinit var memberFollowEpisodeService: MemberFollowEpisodeService

    override fun convert(from: Member): MemberDto {
        val tokenDto = convert(from, TokenDto::class.java)

        return MemberDto(
            uuid = from.uuid!!,
            token = tokenDto.token!!,
            creationDateTime = from.creationDateTime.withUTCString(),
            lastUpdateDateTime = from.lastUpdateDateTime.withUTCString(),
            isPrivate = from.isPrivate,
            followedAnimes = memberFollowAnimeService.getAllFollowedAnimesUUID(from),
            followedEpisodes = memberFollowEpisodeService.getAllFollowedEpisodesUUID(from),
            totalDuration = memberFollowEpisodeService.getTotalDuration(from),
        )
    }
}