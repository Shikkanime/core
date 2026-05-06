package fr.shikkanime.factories

import fr.shikkanime.dtos.member.RefreshMemberDto
import fr.shikkanime.entities.Member

interface IRefreshMemberFactory : IGenericFactory<Member, RefreshMemberDto> {
    suspend fun toDto(entity: Member, limit: Int): RefreshMemberDto

    override suspend fun toDto(entity: Member) = toDto(entity, 9)
}