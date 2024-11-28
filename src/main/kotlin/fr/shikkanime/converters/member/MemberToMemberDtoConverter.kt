package fr.shikkanime.converters.member

import com.google.inject.Inject
import fr.shikkanime.converters.AbstractConverter
import fr.shikkanime.dtos.member.MemberDto
import fr.shikkanime.dtos.member.TokenDto
import fr.shikkanime.entities.Member
import fr.shikkanime.services.ImageService
import fr.shikkanime.services.MemberFollowAnimeService
import fr.shikkanime.services.MemberFollowEpisodeService
import fr.shikkanime.utils.withUTCString

class MemberToMemberDtoConverter : AbstractConverter<Member, MemberDto>() {
    @Inject
    private lateinit var memberFollowAnimeService: MemberFollowAnimeService

    @Inject
    private lateinit var memberFollowEpisodeService: MemberFollowEpisodeService

    @Converter
    fun convert(from: Member): MemberDto {
        val tokenDto = convert(from, TokenDto::class.java)
        val seenAndUnseenDuration = memberFollowEpisodeService.getSeenAndUnseenDuration(from)

        return MemberDto(
            uuid = from.uuid!!,
            token = tokenDto.token!!,
            creationDateTime = from.creationDateTime.withUTCString(),
            lastUpdateDateTime = from.lastUpdateDateTime.withUTCString(),
            isPrivate = from.isPrivate,
            email = from.email,
            followedAnimes = memberFollowAnimeService.findAllFollowedAnimesUUID(from).toSet(),
            followedEpisodes = memberFollowEpisodeService.findAllFollowedEpisodesUUID(from).toSet(),
            totalDuration = seenAndUnseenDuration.first,
            totalUnseenDuration = seenAndUnseenDuration.second,
            hasProfilePicture = ImageService[from.uuid, ImageService.Type.IMAGE] != null
        )
    }
}