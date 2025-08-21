package fr.shikkanime.services.caches

import com.google.gson.reflect.TypeToken
import com.google.inject.Inject
import fr.shikkanime.caches.CountryCodeLocalDateKeyCache
import fr.shikkanime.caches.CountryCodeNamePaginationKeyCache
import fr.shikkanime.caches.CountryCodeUUIDSortPaginationKeyCache
import fr.shikkanime.dtos.PageableDto
import fr.shikkanime.dtos.animes.AnimeDto
import fr.shikkanime.dtos.weekly.WeeklyAnimesDto
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
import fr.shikkanime.utils.MapCacheValue
import fr.shikkanime.utils.SerializationUtils
import fr.shikkanime.utils.StringUtils
import java.time.LocalDate
import java.time.ZonedDateTime
import java.util.*

class AnimeCacheService : ICacheService {
    @Inject private lateinit var animeService: AnimeService
    @Inject private lateinit var simulcastCacheService: SimulcastCacheService
    @Inject private lateinit var memberCacheService: MemberCacheService
    @Inject private lateinit var animeFactory: AnimeFactory

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
            typeToken = object : TypeToken<MapCacheValue<PageableDto<AnimeDto>>>() {},
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
        typeToken = object : TypeToken<MapCacheValue<Array<String>>>() {},
        serializationType = SerializationUtils.SerializationType.JSON,
        key = anime.uuid!!,
    ) { uuid -> animeService.findAllAudioLocales(uuid).toTypedArray() }

    fun getSeasons(anime: Anime) = MapCache.getOrCompute(
        "AnimeCacheService.getSeasons",
        classes = listOf(Anime::class.java, EpisodeMapping::class.java, EpisodeVariant::class.java),
        typeToken = object : TypeToken<MapCacheValue<HashMap<Int, ZonedDateTime>>>() {},
        serializationType = SerializationUtils.SerializationType.JSON,
        key = anime.uuid!!,
    ) { uuid -> HashMap(animeService.findAllSeasons(uuid)) }

    fun find(uuid: UUID) = MapCache.getOrComputeNullable(
        "AnimeCacheService.find",
        classes = listOf(Anime::class.java),
        typeToken = object : TypeToken<MapCacheValue<Anime>>() {},
        serializationType = SerializationUtils.SerializationType.OBJECT,
        key = uuid,
    ) { animeService.find(it) }

    fun findBySlug(countryCode: CountryCode, slug: String) = MapCache.getOrComputeNullable(
        "AnimeCacheService.findBySlug",
        classes = listOf(Anime::class.java, EpisodeMapping::class.java, EpisodeVariant::class.java),
        typeToken = object : TypeToken<MapCacheValue<AnimeDto>>() {},
        key = countryCode to slug,
    ) { animeService.findBySlug(it.first, it.second)?.let { anime -> animeFactory.toDto(anime) } }

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
                it.member?.let { uuid -> memberCacheService.find(uuid) },
                it.localDate,
                it.searchTypes,
            ).toTypedArray()
        }
}