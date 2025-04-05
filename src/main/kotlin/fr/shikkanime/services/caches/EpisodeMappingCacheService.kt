package fr.shikkanime.services.caches

import com.google.inject.Inject
import fr.shikkanime.caches.CountryCodePaginationKeyCache
import fr.shikkanime.caches.CountryCodeUUIDSeasonSortPaginationKeyCache
import fr.shikkanime.dtos.PageableDto
import fr.shikkanime.entities.EpisodeMapping
import fr.shikkanime.entities.EpisodeVariant
import fr.shikkanime.entities.enums.CountryCode
import fr.shikkanime.entities.enums.EpisodeType
import fr.shikkanime.entities.miscellaneous.SortParameter
import fr.shikkanime.factories.impl.EpisodeMappingFactory
import fr.shikkanime.factories.impl.GroupedEpisodeFactory
import fr.shikkanime.services.EpisodeMappingService
import fr.shikkanime.utils.MapCache
import java.util.*

class EpisodeMappingCacheService : ICacheService {
    companion object {
        private const val DEFAULT_ALL_KEY = "all"
    }

    @Inject
    private lateinit var episodeMappingService: EpisodeMappingService

    @Inject
    private lateinit var animeCacheService: AnimeCacheService

    @Inject
    private lateinit var episodeMappingFactory: EpisodeMappingFactory

    @Inject
    private lateinit var groupedEpisodeFactory: GroupedEpisodeFactory

    fun findAllBy(
        countryCode: CountryCode?,
        anime: UUID?,
        season: Int?,
        sort: List<SortParameter>,
        page: Int,
        limit: Int,
    ) = MapCache.getOrCompute(
        "EpisodeMappingCacheService.findAllBy",
        classes = listOf(EpisodeMapping::class.java, EpisodeVariant::class.java),
        key = CountryCodeUUIDSeasonSortPaginationKeyCache(countryCode, anime, season, sort, page, limit),
    ) {
        PageableDto.fromPageable(
            episodeMappingService.findAllBy(
                it.countryCode,
                it.uuid?.let { uuid -> animeCacheService.find(uuid) },
                it.season,
                it.sort,
                it.page,
                it.limit,
            ),
            episodeMappingFactory
        )
    }

    fun findAllGroupedBy(
        countryCode: CountryCode,
        page: Int,
        limit: Int,
    ) = MapCache.getOrCompute(
        "EpisodeMappingCacheService.findAllGroupedBy",
        classes = listOf(EpisodeMapping::class.java, EpisodeVariant::class.java),
        key = CountryCodePaginationKeyCache(countryCode, page, limit),
    ) {
        PageableDto.fromPageable(
            episodeMappingService.findAllGrouped(
                it.countryCode!!,
                it.page,
                it.limit,
            ),
            groupedEpisodeFactory
        )
    }

    fun findAllSeo() = MapCache.getOrCompute(
        "EpisodeMappingCacheService.findAllSeo",
        classes = listOf(EpisodeMapping::class.java),
        key = DEFAULT_ALL_KEY,
    ) { episodeMappingService.findAllSeo() }

    fun findPreviousAndNextBy(
        animeUuid: UUID,
        season: Int,
        episodeType: EpisodeType,
        number: Int
    ) = MapCache.getOrCompute(
        "EpisodeMappingCacheService.findPreviousAndNextBy",
        classes = listOf(EpisodeMapping::class.java),
        key = animeUuid,
    ) {
        val result = episodeMappingService.findAllByAnime(it)

        result.mapIndexed { index, current ->
            Triple(current.season!!, current.episodeType!!, current.number!!) to Triple(
                result.getOrNull(index - 1)?.let { episodeMappingFactory.toDto(it) },
                episodeMappingFactory.toDto(current),
                result.getOrNull(index + 1)?.let { episodeMappingFactory.toDto(it) }
            )
        }.toMap()
    }[Triple(season, episodeType, number)]

    fun findMinimalReleaseDateTime() = MapCache.getOrCompute(
        "EpisodeMappingCacheService.findMinimalReleaseDateTime",
        classes = listOf(EpisodeMapping::class.java),
        key = DEFAULT_ALL_KEY,
    ) { episodeMappingService.findMinimalReleaseDateTime() }
}