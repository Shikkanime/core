package fr.shikkanime.factories.impl

import com.google.inject.Inject
import fr.shikkanime.dtos.member.MemberDto
import fr.shikkanime.dtos.member.TokenDto
import fr.shikkanime.entities.Member
import fr.shikkanime.entities.enums.ImageType
import fr.shikkanime.factories.IGenericFactory
import fr.shikkanime.services.AttachmentService
import fr.shikkanime.services.MemberFollowAnimeService
import fr.shikkanime.services.MemberFollowEpisodeService
import fr.shikkanime.utils.withUTCString

class MemberFactory : IGenericFactory<Member, MemberDto> {
    @Inject private lateinit var memberFollowAnimeService: MemberFollowAnimeService
    @Inject private lateinit var memberFollowEpisodeService: MemberFollowEpisodeService
    @Inject private lateinit var attachmentService: AttachmentService

    override fun toDto(entity: Member): MemberDto {
        val seenAndUnseenDuration = memberFollowEpisodeService.getSeenAndUnseenDuration(entity)
        val attachment = attachmentService.findByEntityUuidTypeAndActive(entity.uuid!!, ImageType.MEMBER_PROFILE)

        return MemberDto(
            uuid = entity.uuid,
            token = TokenDto.build(entity).token!!,
            creationDateTime = entity.creationDateTime.withUTCString(),
            lastUpdateDateTime = entity.lastUpdateDateTime.withUTCString(),
            isPrivate = entity.isPrivate,
            email = entity.email,
            followedAnimes = memberFollowAnimeService.findAllFollowedAnimesUUID(entity.uuid).toSet(),
            followedEpisodes = memberFollowEpisodeService.findAllFollowedEpisodesUUID(entity.uuid).toSet(),
            totalDuration = seenAndUnseenDuration.first,
            totalUnseenDuration = seenAndUnseenDuration.second,
            hasProfilePicture = attachment != null,
            attachmentLastUpdateDateTime = attachment?.lastUpdateDateTime?.withUTCString()
        )
    }
}