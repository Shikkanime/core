package fr.shikkanime.services.caches

import com.google.inject.Inject
import fr.shikkanime.converters.AbstractConverter
import fr.shikkanime.dtos.MemberDto
import fr.shikkanime.entities.*
import fr.shikkanime.services.MemberService
import fr.shikkanime.utils.MapCache
import java.time.Duration
import java.util.*

class MemberCacheService : AbstractCacheService {
    @Inject
    private lateinit var memberService: MemberService

    private val cache = MapCache<UUID, Member?>(classes = listOf(Member::class.java)) {
        memberService.find(it)
    }

    private val findByIdentifierCache =
        MapCache<String, MemberDto?>(
            duration = Duration.ofHours(1),
            classes = listOf(
                Member::class.java,
                MemberFollowAnime::class.java,
                MemberFollowEpisode::class.java,
                Anime::class.java,
                EpisodeMapping::class.java,
            ),
        ) {
            memberService.findByIdentifier(it)
                ?.let { member -> AbstractConverter.convert(member, MemberDto::class.java) }
        }

    fun find(uuid: UUID) = cache[uuid]

    fun findByIdentifier(identifier: String) = findByIdentifierCache[identifier]
}