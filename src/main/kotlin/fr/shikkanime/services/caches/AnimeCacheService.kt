package fr.shikkanime.services.caches

import com.google.gson.reflect.TypeToken
import com.google.inject.Inject
import fr.shikkanime.caches.CountryCodeLocalDateKeyCache
import fr.shikkanime.caches.CountryCodeNamePaginationKeyCache
import fr.shikkanime.caches.CountryCodeUUIDSortPaginationKeyCache
import fr.shikkanime.dtos.PageableDto
import fr.shikkanime.dtos.SeasonDto
import fr.shikkanime.dtos.animes.AnimeDto
import fr.shikkanime.dtos.weekly.WeeklyAnimesDto
import fr.shikkanime.entities.*
import fr.shikkanime.entities.enums.CountryCode
import fr.shikkanime.entities.enums.LangType
import fr.shikkanime.entities.miscellaneous.SortParameter
import fr.shikkanime.factories.impl.AnimeFactory
import fr.shikkanime.factories.impl.SeasonFactory
import fr.shikkanime.services.AnimeService
import fr.shikkanime.utils.MapCache
import fr.shikkanime.utils.MapCacheValue
import fr.shikkanime.utils.SerializationUtils
import fr.shikkanime.utils.StringUtils
import java.time.LocalDate
import java.util.*

class AnimeCacheService : ICacheService {
    @Inject private lateinit var animeService: AnimeService
    @Inject private lateinit var simulcastCacheService: SimulcastCacheService
    @Inject private lateinit var animeFactory: AnimeFactory
    @Inject private lateinit var seasonFactory: SeasonFactory

    fun findAll() = MapCache.getOrCompute(
        "AnimeCacheService.findAll",
        classes = listOf(Anime::class.java),
        typeToken = object : TypeToken<MapCacheValue<Array<Anime>>>() {},
        serializationType = SerializationUtils.SerializationType.OBJECT,
        key = StringUtils.EMPTY_STRING,
    ) { animeService.findAll().toTypedArray() }

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
        typeToken = object : TypeToken<MapCacheValue<PageableDto<AnimeDto>>>() {},
        key = CountryCodeUUIDSortPaginationKeyCache(countryCode, uuid, sort, page, limit, searchTypes),
    ) {
        PageableDto.fromPageable(
            animeService.findAllBy(
                it.countryCode,
                it.uuid,
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
            typeToken = object : TypeToken<MapCacheValue<PageableDto<AnimeDto>>>() {},
            key = CountryCodeNamePaginationKeyCache(countryCode, name, page, limit, searchTypes),
        ) {
            PageableDto.fromPageable(
                animeService.findAllByName(it.countryCode, it.name, it.page, it.limit, it.searchTypes),
                animeFactory
            )
        }

    private fun findAllBySimulcast(simulcastUuid: UUID) = MapCache.getOrCompute(
        "AnimeCacheService.findAllBySimulcast",
        classes = listOf(Anime::class.java, EpisodeMapping::class.java, EpisodeVariant::class.java, Simulcast::class.java),
        typeToken = object : TypeToken<MapCacheValue<Array<AnimeDto>>>() {},
        key = simulcastUuid
    ) {
        animeService.findAllBySimulcast(it)
            .map(animeFactory::toDto)
            .toTypedArray()
    }

    fun findAllByCurrentSimulcastAndLastSimulcast(): Array<AnimeDto> {
        return simulcastCacheService.findAll()
            .take(2)
            .mapNotNull { it.uuid }
            .flatMap { findAllBySimulcast(it).asIterable() }
            .toTypedArray()
    }

    fun getAudioLocales(animeUuid: UUID) = MapCache.getOrCompute(
        "AnimeCacheService.getAudioLocales",
        classes = listOf(Anime::class.java, EpisodeMapping::class.java, EpisodeVariant::class.java),
        typeToken = object : TypeToken<MapCacheValue<Array<String>>>() {},
        serializationType = SerializationUtils.SerializationType.JSON,
        key = animeUuid,
    ) { uuid -> animeService.findAllAudioLocales(uuid).distinct().toTypedArray() }

    fun getLangTypes(anime: Anime) = MapCache.getOrCompute(
        "AnimeCacheService.getLangTypes",
        classes = listOf(Anime::class.java, EpisodeMapping::class.java, EpisodeVariant::class.java),
        typeToken = object : TypeToken<MapCacheValue<Array<LangType>>>() {},
        serializationType = SerializationUtils.SerializationType.JSON,
        key = anime.countryCode!! to anime.uuid!!,
    ) { (countryCode, uuid) -> getAudioLocales(uuid).map { LangType.fromAudioLocale(countryCode, it) }.sorted().toTypedArray() }

    fun findAllSeasons(anime: Anime) = MapCache.getOrCompute(
        "AnimeCacheService.findAllSeasons",
        classes = listOf(Anime::class.java, EpisodeMapping::class.java, EpisodeVariant::class.java),
        typeToken = object : TypeToken<MapCacheValue<Array<SeasonDto>>>() {},
        serializationType = SerializationUtils.SerializationType.JSON,
        key = anime.uuid!!,
    ) { uuid -> animeService.findAllSeasons(uuid).map(seasonFactory::toDto).toTypedArray() }

    fun findBySlug(countryCode: CountryCode, slug: String) = MapCache.getOrComputeNullable(
        "AnimeCacheService.findBySlug",
        classes = listOf(Anime::class.java, EpisodeMapping::class.java, EpisodeVariant::class.java),
        typeToken = object : TypeToken<MapCacheValue<AnimeDto>>() {},
        key = countryCode to slug,
    ) { animeService.findBySlug(it.first, it.second)?.let(animeFactory::toDto) }

    fun findByName(countryCode: CountryCode, name: String) = MapCache.getOrComputeNullable(
        "AnimeCacheService.findByName",
        classes = listOf(Anime::class.java, EpisodeMapping::class.java, EpisodeVariant::class.java),
        typeToken = object : TypeToken<MapCacheValue<AnimeDto>>() {},
        key = countryCode to name,
    ) { animeService.findByName(it.first, it.second)?.let(animeFactory::toDto) }

    fun getWeeklyAnimes(countryCode: CountryCode, memberUuid: UUID?, startOfWeekDay: LocalDate, searchTypes: Array<LangType>? = null) =
        MapCache.getOrCompute(
            "AnimeCacheService.getWeeklyAnimes",
            classes = listOf(Anime::class.java, EpisodeMapping::class.java, EpisodeVariant::class.java, MemberFollowAnime::class.java),
            typeToken = object : TypeToken<MapCacheValue<Array<WeeklyAnimesDto>>>() {},
            serializationType = SerializationUtils.SerializationType.JSON,
            key = CountryCodeLocalDateKeyCache(countryCode, memberUuid, startOfWeekDay, searchTypes),
        ) {
            animeService.getWeeklyAnimes(
                it.countryCode,
                it.member,
                it.localDate,
                it.searchTypes,
            ).toTypedArray()
        }
}