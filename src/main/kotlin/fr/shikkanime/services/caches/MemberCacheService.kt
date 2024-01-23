package fr.shikkanime.services.caches

import com.google.inject.Inject
import fr.shikkanime.entities.Member
import fr.shikkanime.services.MemberService
import fr.shikkanime.utils.MapCache
import java.util.*

class MemberCacheService : AbstractCacheService() {
    @Inject
    private lateinit var memberService: MemberService

    private val cache = MapCache<UUID, Member?>(classes = listOf(Member::class.java)) {
        memberService.find(it)
    }

    fun find(uuid: UUID) = cache[uuid]
}