package fr.shikkanime.services.caches

import com.google.inject.Inject
import fr.shikkanime.converters.AbstractConverter
import fr.shikkanime.dtos.MemberDto
import fr.shikkanime.entities.Member
import fr.shikkanime.entities.MemberFollowAnime
import fr.shikkanime.entities.MemberFollowEpisode
import fr.shikkanime.services.MemberService
import fr.shikkanime.utils.MapCache
import java.util.*

class MemberCacheService : AbstractCacheService {
    @Inject
    private lateinit var memberService: MemberService

    private val cache = MapCache<UUID, Member?>(classes = listOf(Member::class.java)) {
        memberService.find(it)
    }

    private val findPrivateMemberCache =
        MapCache<String, MemberDto?>(classes = listOf(Member::class.java, MemberFollowAnime::class.java, MemberFollowEpisode::class.java)) {
            memberService.findPrivateMember(it)
                ?.let { member -> AbstractConverter.convert(member, MemberDto::class.java) }
        }

    fun find(uuid: UUID) = cache[uuid]

    fun findPrivateMember(identifier: String) = findPrivateMemberCache[identifier]
}