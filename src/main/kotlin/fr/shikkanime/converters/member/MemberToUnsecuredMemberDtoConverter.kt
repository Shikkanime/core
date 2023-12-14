package fr.shikkanime.converters.member

import fr.shikkanime.converters.AbstractConverter
import fr.shikkanime.dtos.UnsecuredMemberDto
import fr.shikkanime.entities.Member
import java.time.format.DateTimeFormatter

class MemberToUnsecuredMemberDtoConverter : AbstractConverter<Member, UnsecuredMemberDto>() {
    override fun convert(from: Member): UnsecuredMemberDto {
        return UnsecuredMemberDto(
            uuid = from.uuid,
            creationDateTime = from.creationDateTime.format(DateTimeFormatter.ISO_DATE_TIME),
            username = from.username!!,
            password = from.password!!,
            role = from.role,
        )
    }
}