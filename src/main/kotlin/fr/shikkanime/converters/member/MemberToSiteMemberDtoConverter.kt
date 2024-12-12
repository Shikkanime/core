package fr.shikkanime.converters.member

import com.google.inject.Inject
import fr.shikkanime.converters.AbstractConverter
import fr.shikkanime.dtos.member.SiteMemberDto
import fr.shikkanime.entities.Member
import fr.shikkanime.services.ImageService
import fr.shikkanime.services.MemberFollowEpisodeService
import fr.shikkanime.utils.withUTCString

class MemberToSiteMemberDtoConverter : AbstractConverter<Member, SiteMemberDto>() {
    @Inject
    private lateinit var memberFollowEpisodeService: MemberFollowEpisodeService

    @Converter
    fun convert(from: Member): SiteMemberDto {
        val seenAndUnseenDuration = memberFollowEpisodeService.getSeenAndUnseenDuration(from)

        return SiteMemberDto(
            uuid = from.uuid!!,
            creationDateTime = from.creationDateTime.withUTCString(),
            username = from.username,
            totalDuration = seenAndUnseenDuration.first,
            totalUnseenDuration = seenAndUnseenDuration.second,
            hasProfilePicture = ImageService[from.uuid, ImageService.Type.IMAGE] != null
        )
    }
}