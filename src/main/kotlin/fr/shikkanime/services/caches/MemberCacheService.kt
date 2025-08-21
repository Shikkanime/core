package fr.shikkanime.services.caches

import com.google.gson.reflect.TypeToken
import com.google.inject.Inject
import fr.shikkanime.caches.UUIDPaginationKeyCache
import fr.shikkanime.dtos.member.RefreshMemberDto
import fr.shikkanime.entities.*
import fr.shikkanime.factories.impl.RefreshMemberFactory
import fr.shikkanime.services.MemberService
import fr.shikkanime.utils.MapCache
import fr.shikkanime.utils.MapCacheValue
import fr.shikkanime.utils.SerializationUtils
import java.util.*

class MemberCacheService : ICacheService {
    @Inject private lateinit var memberService: MemberService
    @Inject private lateinit var refreshMemberFactory: RefreshMemberFactory

    fun find(uuid: UUID) = MapCache.getOrComputeNullable(
        "MemberCacheService.find",
        classes = listOf(Member::class.java),
        typeToken = object : TypeToken<MapCacheValue<Member>>() {},
        serializationType = SerializationUtils.SerializationType.OBJECT,
        key = uuid
    ) { memberService.find(it) }

    fun getRefreshMember(uuid: UUID, limit: Int) = MapCache.getOrComputeNullable(
        "MemberCacheService.getRefreshMember",
        classes = listOf(Member::class.java, Anime::class.java, MemberFollowAnime::class.java, EpisodeMapping::class.java, MemberFollowEpisode::class.java),
        typeToken = object : TypeToken<MapCacheValue<RefreshMemberDto>>() {},
        key = UUIDPaginationKeyCache(uuid, 1, limit)
    ) { find(it.uuid)?.let { member -> refreshMemberFactory.toDto(member, it.limit) } }
}