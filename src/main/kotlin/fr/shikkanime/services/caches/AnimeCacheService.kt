package fr.shikkanime.services.caches

import com.google.inject.Inject
import fr.shikkanime.caches.CountryCodeLocalDateKeyCache
import fr.shikkanime.caches.CountryCodeNamePaginationKeyCache
import fr.shikkanime.caches.CountryCodeUUIDSortPaginationKeyCache
import fr.shikkanime.dtos.PageableDto
import fr.shikkanime.entities.Anime
import fr.shikkanime.entities.EpisodeMapping
import fr.shikkanime.entities.EpisodeVariant
import fr.shikkanime.entities.MemberFollowAnime
import fr.shikkanime.entities.enums.CountryCode
import fr.shikkanime.entities.enums.LangType
import fr.shikkanime.entities.miscellaneous.SortParameter
import fr.shikkanime.factories.impl.AnimeFactory
import fr.shikkanime.services.AnimeService
import fr.shikkanime.utils.MapCache
import java.time.LocalDate
import java.util.*

class AnimeCacheService : ICacheService {
    companion object {
        private const val DEFAULT_ALL_KEY = "all"
    }

    @Inject
    private lateinit var animeService: AnimeService

    @Inject
    private lateinit var simulcastCacheService: SimulcastCacheService

    @Inject
    private lateinit var memberCacheService: MemberCacheService

    @Inject
    private lateinit var animeFactory: AnimeFactory

    fun findAll() = MapCache.getOrCompute(
        "AnimeCacheService.findAll",
        classes = listOf(Anime::class.java),
        key = DEFAULT_ALL_KEY,
    ) { animeService.findAll() }

    fun findAllBy(
        countryCode: CountryCode?,
        uuid: UUID?,
        sort: List<SortParameter>,
        page: Int,
        limit: Int,
        searchTypes: Array<LangType>? = null,
    ) = MapCache.getOrCompute(
        "AnimeCacheService.findAllBy",
        classes = listOf(Anime::class.java, EpisodeMapping::class.java, EpisodeVariant::class.java),
        key = CountryCodeUUIDSortPaginationKeyCache(countryCode, uuid, sort, page, limit, searchTypes),
    ) {
        PageableDto.fromPageable(
            animeService.findAllBy(
                it.countryCode,
                it.uuid?.let { uuid -> simulcastCacheService.find(uuid) },
                it.sort,
                it.page,
                it.limit,
                it.searchTypes,
            ),
            animeFactory
        )
    }

    fun findAllByName(countryCode: CountryCode?, name: String, page: Int, limit: Int, searchTypes: Array<LangType>?) =
        MapCache.getOrCompute(
            "AnimeCacheService.findAllByName",
            classes = listOf(Anime::class.java, EpisodeMapping::class.java, EpisodeVariant::class.java),
            key = CountryCodeNamePaginationKeyCache(countryCode, name, page, limit, searchTypes),
        ) {
            PageableDto.fromPageable(
                animeService.findAllByName(it.countryCode, it.name, it.page, it.limit, it.searchTypes),
                animeFactory
            )
        }

    fun getAudioLocales(anime: Anime) = MapCache.getOrCompute(
        "AnimeCacheService.getAudioLocales",
        classes = listOf(Anime::class.java, EpisodeMapping::class.java, EpisodeVariant::class.java),
        key = DEFAULT_ALL_KEY,
    ) { animeService.findAllAudioLocales() }[anime.uuid!!]

    fun getSeasons(anime: Anime) = MapCache.getOrCompute(
        "AnimeCacheService.getSeasons",
        classes = listOf(Anime::class.java, EpisodeMapping::class.java, EpisodeVariant::class.java),
        key = DEFAULT_ALL_KEY,
    ) { animeService.findAllSeasons() }[anime.uuid!!]

    fun find(uuid: UUID) = MapCache.getOrComputeNullable(
        "AnimeCacheService.find",
        classes = listOf(Anime::class.java),
        key = uuid,
    ) { animeService.find(it) }

    fun findBySlug(countryCode: CountryCode, slug: String) = MapCache.getOrComputeNullable(
        "AnimeCacheService.findBySlug",
        classes = listOf(Anime::class.java, EpisodeMapping::class.java, EpisodeVariant::class.java),
        key = countryCode to slug,
    ) {
        animeService.findBySlug(it.first, it.second)?.let { anime -> animeFactory.toDto(anime) }
    }

    fun getWeeklyAnimes(countryCode: CountryCode, memberUuid: UUID?, startOfWeekDay: LocalDate, searchTypes: Array<LangType>? = null) =
        MapCache.getOrCompute(
            "AnimeCacheService.getWeeklyAnimes",
            classes = listOf(Anime::class.java, EpisodeMapping::class.java, EpisodeVariant::class.java, MemberFollowAnime::class.java),
            key = CountryCodeLocalDateKeyCache(countryCode, memberUuid, startOfWeekDay, searchTypes),
        ) {
            animeService.getWeeklyAnimes(
                it.countryCode,
                it.member?.let { uuid -> memberCacheService.find(uuid) },
                it.localDate,
                it.searchTypes,
            )
        }
}