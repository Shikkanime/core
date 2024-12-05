package fr.shikkanime.services.caches

import com.google.inject.Inject
import fr.shikkanime.caches.AnimeUUIDSeasonEpisodeTypeNumberKeyCache
import fr.shikkanime.caches.CountryCodeUUIDSeasonSortPaginationKeyCache
import fr.shikkanime.converters.AbstractConverter
import fr.shikkanime.dtos.PageableDto
import fr.shikkanime.dtos.enums.Status
import fr.shikkanime.dtos.mappings.EpisodeMappingDto
import fr.shikkanime.dtos.mappings.EpisodeMappingSeoDto
import fr.shikkanime.entities.EpisodeMapping
import fr.shikkanime.entities.EpisodeVariant
import fr.shikkanime.entities.SortParameter
import fr.shikkanime.entities.enums.CountryCode
import fr.shikkanime.entities.enums.EpisodeType
import fr.shikkanime.services.AnimeService
import fr.shikkanime.services.EpisodeMappingService
import fr.shikkanime.utils.MapCache
import java.util.UUID

class EpisodeMappingCacheService : AbstractCacheService {
    companion object {
        private const val DEFAULT_ALL_KEY = "all"
    }

    @Inject
    private lateinit var episodeMappingService: EpisodeMappingService

    @Inject
    private lateinit var animeService: AnimeService
    
    @Inject
    private lateinit var episodeVariantCacheService: EpisodeVariantCacheService

    @Inject
    private lateinit var animeCacheService: AnimeCacheService

    private val findAllCache = MapCache<String, List<EpisodeMapping>>(
        "EpisodeMappingCacheService.findAllCache",
        classes = listOf(EpisodeMapping::class.java),
        fn = { listOf(DEFAULT_ALL_KEY) },
        requiredCaches = { listOf(episodeVariantCacheService.findAllCache) }
    ) {
        (episodeVariantCacheService.findAllCache[DEFAULT_ALL_KEY] ?: emptyList()).asSequence()
            .map { it.mapping!! }
            .distinctBy { it.uuid }
            .toList()
    }

    private val findAllByCache =
        MapCache<CountryCodeUUIDSeasonSortPaginationKeyCache, PageableDto<EpisodeMappingDto>>(
            "EpisodeMappingCacheService.findAllByCache",
            classes = listOf(
                EpisodeMapping::class.java,
                EpisodeVariant::class.java
            )
        ) {
            PageableDto.fromPageable(
                episodeMappingService.findAllBy(
                    it.countryCode,
                    animeService.find(it.uuid),
                    it.season,
                    it.sort,
                    it.page,
                    it.limit,
                    it.status
                ),
                EpisodeMappingDto::class.java
            )
        }

    private val findAllSeoCache = MapCache<String, List<EpisodeMappingSeoDto>>(
        "EpisodeMappingCacheService.findAllSeoCache",
        classes = listOf(EpisodeMapping::class.java),
        fn = { listOf(DEFAULT_ALL_KEY) },
        requiredCaches = { listOf(findAllCache) }
    ) {
        AbstractConverter.convert(findAllCache[DEFAULT_ALL_KEY] ?: emptyList(), EpisodeMappingSeoDto::class.java)!!
    }

    private val findPreviousAndNextEpisodeCache = MapCache<String, Map<AnimeUUIDSeasonEpisodeTypeNumberKeyCache, Triple<EpisodeMappingDto?, EpisodeMappingDto, EpisodeMappingDto?>>>(
        "EpisodeMappingCacheService.findPreviousAndNextEpisodeCache",
        classes = listOf(EpisodeMapping::class.java, EpisodeVariant::class.java),
        fn = { listOf(DEFAULT_ALL_KEY) },
        requiredCaches = { listOf(findAllCache, episodeVariantCacheService.findAllByEpisodeMappingCache, animeCacheService.findAllAudioLocalesAndSeasonsCache) }
    ) {
        val map = mutableMapOf<AnimeUUIDSeasonEpisodeTypeNumberKeyCache, Triple<EpisodeMappingDto?, EpisodeMappingDto, EpisodeMappingDto?>>()

        (findAllCache[DEFAULT_ALL_KEY] ?: emptyList())
            .sortedWith(
                compareBy(
                    { it.releaseDateTime },
                    { it.season },
                    { it.episodeType },
                    { it.number }
                )
            )
            .groupBy { it.anime!!.uuid!! }
            .values.forEach { groupedEpisodes ->
                val convertedGroup = groupedEpisodes.map { AbstractConverter.convert(it, EpisodeMappingDto::class.java) }

                convertedGroup.forEachIndexed { index, current ->
                    map[AnimeUUIDSeasonEpisodeTypeNumberKeyCache(
                        current.anime.uuid!!,
                        current.season,
                        current.episodeType,
                        current.number
                    )] = Triple(
                        convertedGroup.getOrNull(index - 1),
                        current,
                        convertedGroup.getOrNull(index + 1)
                    )
                }
            }

        map
    }

    fun findAll() = findAllCache[DEFAULT_ALL_KEY] ?: emptyList()

    fun findAllBy(
        countryCode: CountryCode?,
        anime: UUID?,
        season: Int?,
        sort: List<SortParameter>,
        page: Int,
        limit: Int,
        status: Status? = null
    ) = findAllByCache[CountryCodeUUIDSeasonSortPaginationKeyCache(countryCode, anime, season, sort, page, limit, status)]

    fun findAllSeo() = findAllSeoCache[DEFAULT_ALL_KEY]

    fun findPreviousAndNextBy(
        animeUuid: UUID,
        season: Int,
        episodeType: EpisodeType,
        number: Int
    ) = (findPreviousAndNextEpisodeCache[DEFAULT_ALL_KEY] ?: emptyMap())[AnimeUUIDSeasonEpisodeTypeNumberKeyCache(animeUuid, season, episodeType, number)]
}