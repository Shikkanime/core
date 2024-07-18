package fr.shikkanime.services.caches

import com.google.inject.Inject
import fr.shikkanime.caches.CountryCodeSlugSeasonEpisodeTypeNumberKeyCache
import fr.shikkanime.caches.CountryCodeUUIDSeasonSortPaginationKeyCache
import fr.shikkanime.converters.AbstractConverter
import fr.shikkanime.dtos.PageableDto
import fr.shikkanime.dtos.enums.Status
import fr.shikkanime.dtos.mappings.EpisodeMappingDto
import fr.shikkanime.entities.EpisodeMapping
import fr.shikkanime.entities.EpisodeVariant
import fr.shikkanime.entities.SortParameter
import fr.shikkanime.entities.enums.CountryCode
import fr.shikkanime.entities.enums.EpisodeType
import fr.shikkanime.services.AnimeService
import fr.shikkanime.services.EpisodeMappingService
import fr.shikkanime.utils.MapCache
import java.util.*

class EpisodeMappingCacheService : AbstractCacheService {
    @Inject
    private lateinit var episodeMappingService: EpisodeMappingService

    @Inject
    private lateinit var animeService: AnimeService

    private val findAllByCache =
        MapCache<CountryCodeUUIDSeasonSortPaginationKeyCache, PageableDto<EpisodeMappingDto>>(
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

    private val findByAnimeSeasonEpisodeTypeNumberCache =
        MapCache<CountryCodeSlugSeasonEpisodeTypeNumberKeyCache, Triple<EpisodeMappingDto?, EpisodeMappingDto, EpisodeMappingDto?>>(
            classes = listOf(
                EpisodeMapping::class.java,
                EpisodeVariant::class.java
            )
        ) {
            val current = episodeMappingService.findByAnimeSeasonEpisodeTypeNumber(
                it.animeUuid,
                it.season,
                it.episodeType,
                it.number
            ) ?: throw Exception("Episode not found")

            val previous = episodeMappingService.findPreviousEpisode(current)
            val next = episodeMappingService.findNextEpisode(current)

            Triple(
                previous?.let { p -> AbstractConverter.convert(p, EpisodeMappingDto::class.java) },
                AbstractConverter.convert(current, EpisodeMappingDto::class.java),
                next?.let { n -> AbstractConverter.convert(n, EpisodeMappingDto::class.java) }
            )
        }

    private val findNextEpisodeCache = MapCache<UUID, EpisodeMapping?>(classes = listOf(EpisodeMapping::class.java)) {
        episodeMappingService.find(it)?.let { em -> episodeMappingService.findNextEpisode(em) }
    }

    fun findAllBy(
        countryCode: CountryCode?,
        anime: UUID?,
        season: Int?,
        sort: List<SortParameter>,
        page: Int,
        limit: Int,
        status: Status? = null
    ) = findAllByCache[CountryCodeUUIDSeasonSortPaginationKeyCache(countryCode, anime, season, sort, page, limit, status)]

    fun findByAnimeSeasonEpisodeTypeNumber(
        animeUuid: UUID,
        season: Int,
        episodeType: EpisodeType,
        number: Int
    ) = findByAnimeSeasonEpisodeTypeNumberCache[CountryCodeSlugSeasonEpisodeTypeNumberKeyCache(
        animeUuid,
        season,
        episodeType,
        number
    )]

    fun findNextEpisode(uuid: UUID) = findNextEpisodeCache[uuid]
}