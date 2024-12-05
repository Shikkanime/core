package fr.shikkanime.services.caches

import com.google.inject.Inject
import fr.shikkanime.caches.UUIDPaginationKeyCache
import fr.shikkanime.converters.AbstractConverter
import fr.shikkanime.dtos.animes.AnimeDto
import fr.shikkanime.dtos.animes.MissedAnimeDto
import fr.shikkanime.dtos.PageableDto
import fr.shikkanime.entities.Anime
import fr.shikkanime.entities.EpisodeMapping
import fr.shikkanime.entities.MemberFollowAnime
import fr.shikkanime.entities.MemberFollowEpisode
import fr.shikkanime.services.MemberFollowAnimeService
import fr.shikkanime.utils.MapCache
import java.util.*

class MemberFollowAnimeCacheService : AbstractCacheService {
    @Inject
    private lateinit var memberCacheService: MemberCacheService

    @Inject
    private lateinit var memberFollowAnimeService: MemberFollowAnimeService

    private val cache =
        MapCache<UUIDPaginationKeyCache, PageableDto<MissedAnimeDto>?>(
            "MemberFollowAnimeCacheService.cache",
            classes = listOf(
                MemberFollowAnime::class.java,
                MemberFollowEpisode::class.java,
                Anime::class.java,
                EpisodeMapping::class.java,
            ),
        ) {
            val member = memberCacheService.find(it.uuid) ?: return@MapCache null

            val pageable = memberFollowAnimeService.findAllMissedAnimes(
                member,
                it.page,
                it.limit
            )

            val dtos = pageable.data.map { tuple ->
                MissedAnimeDto(
                    AbstractConverter.convert(tuple[0], AnimeDto::class.java),
                    tuple[1] as Long
                )
            }.toSet()

            PageableDto(
                data = dtos,
                page = pageable.page,
                limit = pageable.limit,
                total = pageable.total,
            )
        }

    private val findAllByCache =
        MapCache<UUIDPaginationKeyCache, PageableDto<AnimeDto>>(
            "MemberFollowAnimeCacheService.findAllByCache",
            classes = listOf(
                MemberFollowAnime::class.java,
                Anime::class.java,
            ),
        ) {
            val member = memberCacheService.find(it.uuid) ?: return@MapCache PageableDto.empty()

            val pageable = memberFollowAnimeService.findAllFollowedAnimes(
                member,
                it.page,
                it.limit
            )

            PageableDto.fromPageable(pageable, AnimeDto::class.java)
        }

    fun getMissedAnimes(member: UUID, page: Int, limit: Int) = cache[UUIDPaginationKeyCache(member, page, limit)]

    fun findAllBy(member: UUID, page: Int, limit: Int) = findAllByCache[UUIDPaginationKeyCache(member, page, limit)]
}