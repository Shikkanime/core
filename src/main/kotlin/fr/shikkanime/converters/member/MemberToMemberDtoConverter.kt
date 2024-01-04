package fr.shikkanime.converters.member

import fr.shikkanime.converters.AbstractConverter
import fr.shikkanime.dtos.MemberDto
import fr.shikkanime.entities.Member
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class MemberToMemberDtoConverter : AbstractConverter<Member, MemberDto>() {
    private val utcZone = ZoneId.of("UTC")

    override fun convert(from: Member): MemberDto {
        return MemberDto(
            uuid = from.uuid,
            creationDateTime = from.creationDateTime.withZoneSameInstant(utcZone).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME),
            username = from.username!!,
            role = from.role,
        )
    }
}