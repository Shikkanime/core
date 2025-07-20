package fr.shikkanime.factories

import fr.shikkanime.dtos.member.RefreshMemberDto
import fr.shikkanime.entities.Member

interface IRefreshMemberFactory : IGenericFactory<Member, RefreshMemberDto> {
    fun toDto(entity: Member, limit: Int): RefreshMemberDto

    override fun toDto(entity: Member) = toDto(entity, 9)
}