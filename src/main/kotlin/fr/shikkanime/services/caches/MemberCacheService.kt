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

    private val cache = MapCache<UUID, Member?>(
        "MemberCacheService.cache",
        classes = listOf(Member::class.java)
    ) {
        memberService.find(it)
    }

    private val refreshMemberCache = MapCache<UUIDPaginationKeyCache, RefreshMemberDto?>(
        "MemberCacheService.refreshMemberCache",
        classes = listOf(
            Member::class.java,
            Anime::class.java,
            MemberFollowAnime::class.java,
            EpisodeMapping::class.java,
            MemberFollowEpisode::class.java,
        )
    ) {
        try {
            find(it.uuid)?.let { member -> AbstractConverter.convert(member, RefreshMemberDto::class.java, it.limit) }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun find(uuid: UUID) = cache[uuid]

    fun getRefreshMember(uuid: UUID, limit: Int) = refreshMemberCache[UUIDPaginationKeyCache(uuid, 1, limit)]
}