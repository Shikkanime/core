package fr.shikkanime.services.caches

import com.google.inject.Inject
import fr.shikkanime.caches.CountryCodeSortPaginationKeyCache
import fr.shikkanime.caches.CountryCodeUUIDSeasonSortPaginationKeyCache
import fr.shikkanime.dtos.PageableDto
import fr.shikkanime.entities.Anime
import fr.shikkanime.entities.EpisodeMapping
import fr.shikkanime.entities.EpisodeVariant
import fr.shikkanime.entities.enums.CountryCode
import fr.shikkanime.entities.enums.EpisodeType
import fr.shikkanime.entities.miscellaneous.SortParameter
import fr.shikkanime.factories.impl.EpisodeMappingFactory
import fr.shikkanime.factories.impl.GroupedEpisodeFactory
import fr.shikkanime.services.EpisodeMappingService
import fr.shikkanime.utils.MapCache
import fr.shikkanime.utils.StringUtils
import fr.shikkanime.utils.TelemetryConfig
import fr.shikkanime.utils.TelemetryConfig.trace
import java.util.*

class EpisodeMappingCacheService : ICacheService {
    private val tracer = TelemetryConfig.getTracer("EpisodeMappingCacheService")
    @Inject private lateinit var episodeMappingService: EpisodeMappingService
    @Inject private lateinit var animeCacheService: AnimeCacheService
    @Inject private lateinit var episodeMappingFactory: EpisodeMappingFactory
    @Inject private lateinit var groupedEpisodeFactory: GroupedEpisodeFactory
    @Inject private lateinit var episodeVariantCacheService: EpisodeVariantCacheService

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
    ) { tracer.trace {
        PageableDto.fromPageable(
            episodeMappingService.findAllBy(
                it.countryCode,
                it.uuid?.let { uuid -> animeCacheService.find(uuid) },
                it.season,
                it.sort,
                it.page,
                it.limit,
            ).apply { episodeVariantCacheService.findAllByMappings(*data.toTypedArray()) },
            episodeMappingFactory
        )
    } }

    fun findAllGroupedBy(
        countryCode: CountryCode?,
        sort: List<SortParameter>,
        page: Int,
        limit: Int,
    ) = MapCache.getOrCompute(
        "EpisodeMappingCacheService.findAllGroupedBy",
        classes = listOf(Anime::class.java, EpisodeMapping::class.java, EpisodeVariant::class.java),
        key = CountryCodeSortPaginationKeyCache(countryCode, sort,page, limit),
    ) { tracer.trace {
        PageableDto.fromPageable(
            episodeMappingService.findAllGroupedBy(it.countryCode, it.sort, it.page, it.limit),
            groupedEpisodeFactory
        )
    } }

    fun findAllSeo() = MapCache.getOrCompute(
        "EpisodeMappingCacheService.findAllSeo",
        classes = listOf(EpisodeMapping::class.java),
        key = StringUtils.EMPTY_STRING,
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
    ) { tracer.trace {
        val result = episodeMappingService.findAllByAnime(it)
        episodeVariantCacheService.findAllByMappings(*result.toTypedArray())

        result.mapIndexed { index, current ->
            Triple(current.season!!, current.episodeType!!, current.number!!) to Triple(
                result.getOrNull(index - 1)?.let { episodeMappingFactory.toDto(it) },
                episodeMappingFactory.toDto(current),
                result.getOrNull(index + 1)?.let { episodeMappingFactory.toDto(it) }
            )
        }.toMap()
    } }[Triple(season, episodeType, number)]

    fun findMinimalReleaseDateTime() = MapCache.getOrCompute(
        "EpisodeMappingCacheService.findMinimalReleaseDateTime",
        classes = listOf(EpisodeMapping::class.java),
        key = StringUtils.EMPTY_STRING,
    ) { tracer.trace { episodeMappingService.findMinimalReleaseDateTime() } }
}