package fr.shikkanime.services.caches

import com.google.gson.reflect.TypeToken
import com.google.inject.Inject
import fr.shikkanime.caches.CountryCodeUUIDSeasonSortPaginationKeyCache
import fr.shikkanime.caches.SeasonEpisodeTypeNumberKeyCache
import fr.shikkanime.dtos.PageableDto
import fr.shikkanime.dtos.mappings.EpisodeMappingDto
import fr.shikkanime.dtos.mappings.EpisodeMappingSeoDto
import fr.shikkanime.entities.EpisodeMapping
import fr.shikkanime.entities.EpisodeVariant
import fr.shikkanime.entities.enums.CountryCode
import fr.shikkanime.entities.enums.EpisodeType
import fr.shikkanime.entities.miscellaneous.SortParameter
import fr.shikkanime.factories.impl.EpisodeMappingFactory
import fr.shikkanime.services.EpisodeMappingService
import fr.shikkanime.utils.MapCache
import fr.shikkanime.utils.MapCacheValue
import fr.shikkanime.utils.SerializationUtils
import fr.shikkanime.utils.StringUtils
import java.time.ZonedDateTime
import java.util.*

class EpisodeMappingCacheService : ICacheService {
    @Inject private lateinit var episodeMappingService: EpisodeMappingService
    @Inject private lateinit var episodeMappingFactory: EpisodeMappingFactory

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
        typeToken = object : TypeToken<MapCacheValue<PageableDto<EpisodeMappingDto>>>() {},
        key = CountryCodeUUIDSeasonSortPaginationKeyCache(countryCode, anime, season, sort, page, limit),
    ) {
        PageableDto.fromPageable(
            episodeMappingService.findAllBy(
                it.countryCode,
                it.uuid,
                it.season,
                it.sort,
                it.page,
                it.limit,
            ),
            episodeMappingFactory
        )
    }

    fun findAllSeo() = MapCache.getOrCompute(
        "EpisodeMappingCacheService.findAllSeo",
        classes = listOf(EpisodeMapping::class.java),
        typeToken = object : TypeToken<MapCacheValue<Array<EpisodeMappingSeoDto>>>() {},
        serializationType = SerializationUtils.SerializationType.OBJECT,
        key = StringUtils.EMPTY_STRING,
    ) { episodeMappingService.findAllSeo().toTypedArray() }

    fun findPreviousAndNextBy(
        animeUuid: UUID,
        season: Int,
        episodeType: EpisodeType,
        number: Int
    ) = MapCache.getOrCompute(
        "EpisodeMappingCacheService.findPreviousAndNextBy",
        classes = listOf(EpisodeMapping::class.java),
        typeToken = object : TypeToken<MapCacheValue<HashMap<SeasonEpisodeTypeNumberKeyCache, Triple<EpisodeMappingDto?, EpisodeMappingDto, EpisodeMappingDto?>>>>() {},
        serializationType = SerializationUtils.SerializationType.OBJECT,
        key = animeUuid,
    ) {
        val result = episodeMappingService.findAllByAnime(it)

        HashMap(
            result.mapIndexed { index, current ->
                SeasonEpisodeTypeNumberKeyCache(current.season!!, current.episodeType!!, current.number!!) to Triple(
                    result.getOrNull(index - 1)?.let(episodeMappingFactory::toDto),
                    episodeMappingFactory.toDto(current),
                    result.getOrNull(index + 1)?.let(episodeMappingFactory::toDto)
                )
            }.toMap()
        )
    }[SeasonEpisodeTypeNumberKeyCache(season, episodeType, number)]

    fun findMinimalReleaseDateTime() = MapCache.getOrCompute(
        "EpisodeMappingCacheService.findMinimalReleaseDateTime",
        classes = listOf(EpisodeMapping::class.java),
        typeToken = object : TypeToken<MapCacheValue<ZonedDateTime>>() {},
        key = StringUtils.EMPTY_STRING,
    ) { episodeMappingService.findMinimalReleaseDateTime() }
}