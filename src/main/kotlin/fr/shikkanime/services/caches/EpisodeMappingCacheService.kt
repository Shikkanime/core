package fr.shikkanime.services.caches

import com.google.inject.Inject
import fr.shikkanime.caches.CountryCodePaginationKeyCache
import fr.shikkanime.caches.CountryCodeUUIDSeasonSortPaginationKeyCache
import fr.shikkanime.converters.AbstractConverter
import fr.shikkanime.dtos.PageableDto
import fr.shikkanime.dtos.enums.Status
import fr.shikkanime.dtos.mappings.EpisodeMappingDto
import fr.shikkanime.dtos.mappings.GroupedEpisodeDto
import fr.shikkanime.entities.EpisodeMapping
import fr.shikkanime.entities.EpisodeVariant
import fr.shikkanime.entities.SortParameter
import fr.shikkanime.entities.enums.CountryCode
import fr.shikkanime.entities.enums.EpisodeType
import fr.shikkanime.services.EpisodeMappingService
import fr.shikkanime.utils.MapCache
import java.util.*

class EpisodeMappingCacheService : AbstractCacheService {
    companion object {
        private const val DEFAULT_ALL_KEY = "all"
    }

    @Inject
    private lateinit var episodeMappingService: EpisodeMappingService

    @Inject
    private lateinit var animeCacheService: AnimeCacheService

    fun findAllBy(
        countryCode: CountryCode?,
        anime: UUID?,
        season: Int?,
        sort: List<SortParameter>,
        page: Int,
        limit: Int,
        status: Status? = null
    ) = MapCache.getOrCompute(
        "EpisodeMappingCacheService.findAllBy",
        classes = listOf(EpisodeMapping::class.java, EpisodeVariant::class.java),
        key = CountryCodeUUIDSeasonSortPaginationKeyCache(countryCode, anime, season, sort, page, limit, status),
    ) {
        PageableDto.fromPageable(
            episodeMappingService.findAllBy(
                it.countryCode,
                it.uuid?.let { uuid -> animeCacheService.find(uuid) },
                it.season,
                it.sort,
                it.page,
                it.limit,
                it.status
            ),
            EpisodeMappingDto::class.java
        )
    }

    fun findAllGroupedBy(
        countryCode: CountryCode,
        page: Int,
        limit: Int,
    ) = MapCache.getOrCompute(
        "EpisodeMappingCacheService.findAllGroupedBy",
        key = CountryCodePaginationKeyCache(countryCode, page, limit),
    ) {
        PageableDto.fromPageable(
            episodeMappingService.findAllGrouped(
                it.countryCode!!,
                it.page,
                it.limit,
            ),
            GroupedEpisodeDto::class.java
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
                result.getOrNull(index - 1)?.let { AbstractConverter.convert(it, EpisodeMappingDto::class.java) },
                AbstractConverter.convert(current, EpisodeMappingDto::class.java),
                result.getOrNull(index + 1)?.let { AbstractConverter.convert(it, EpisodeMappingDto::class.java) }
            )
        }.toMap()
    }[Triple(season, episodeType, number)]

    fun findMinimalReleaseDateTime() = MapCache.getOrCompute(
        "EpisodeMappingCacheService.findMinimalReleaseDateTime",
        classes = listOf(EpisodeMapping::class.java),
        key = DEFAULT_ALL_KEY,
    ) { episodeMappingService.findMinimalReleaseDateTime() }
}