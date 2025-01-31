package fr.shikkanime.services.caches

import com.google.inject.Inject
import fr.shikkanime.caches.UUIDPaginationKeyCache
import fr.shikkanime.converters.AbstractConverter
import fr.shikkanime.dtos.member.RefreshMemberDto
import fr.shikkanime.entities.*
import fr.shikkanime.services.MemberService
import fr.shikkanime.utils.MapCache
import java.util.*

class MemberCacheService : AbstractCacheService {
    @Inject
    private lateinit var memberService: MemberService

    fun find(uuid: UUID) = MapCache.getOrComputeNullable(
        "MemberCacheService.find",
        classes = listOf(Member::class.java),
        key = uuid
    ) { memberService.find(it) }

    fun getRefreshMember(uuid: UUID, limit: Int) = MapCache.getOrComputeNullable(
        "MemberCacheService.getRefreshMember",
        classes = listOf(Member::class.java, Anime::class.java, MemberFollowAnime::class.java, EpisodeMapping::class.java, MemberFollowEpisode::class.java),
        key = UUIDPaginationKeyCache(uuid, 1, limit)
    ) { find(it.uuid)?.let { member -> AbstractConverter.convert(member, RefreshMemberDto::class.java, it.limit) } }
}