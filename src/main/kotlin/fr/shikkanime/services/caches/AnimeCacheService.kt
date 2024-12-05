package fr.shikkanime.services.caches

import com.google.inject.Inject
import fr.shikkanime.caches.CountryCodeLocalDateKeyCache
import fr.shikkanime.caches.CountryCodeNamePaginationKeyCache
import fr.shikkanime.caches.CountryCodeUUIDSortPaginationKeyCache
import fr.shikkanime.dtos.animes.AnimeDto
import fr.shikkanime.dtos.PageableDto
import fr.shikkanime.dtos.enums.Status
import fr.shikkanime.dtos.weekly.v1.WeeklyAnimesDto
import fr.shikkanime.entities.*
import fr.shikkanime.entities.enums.CountryCode
import fr.shikkanime.entities.enums.LangType
import fr.shikkanime.services.AnimeService
import fr.shikkanime.services.SimulcastService
import fr.shikkanime.utils.MapCache
import java.time.LocalDate
import java.util.*

class AnimeCacheService : AbstractCacheService {
    companion object {
        private const val DEFAULT_ALL_KEY = "all"
    }

    @Inject
    private lateinit var animeService: AnimeService

    @Inject
    private lateinit var simulcastService: SimulcastService

    @Inject
    private lateinit var memberCacheService: MemberCacheService

    @Inject
    private lateinit var episodeVariantCacheService: EpisodeVariantCacheService

    private val findAllByCache =
        MapCache<CountryCodeUUIDSortPaginationKeyCache, PageableDto<AnimeDto>>(
            "AnimeCacheService.findAllByCache",
            classes = listOf(Anime::class.java)
        ) {
            PageableDto.fromPageable(
                animeService.findAllBy(
                    it.countryCode,
                    simulcastService.find(it.uuid),
                    it.sort,
                    it.page,
                    it.limit,
                    it.searchTypes,
                    it.status
                ),
                AnimeDto::class.java
            )
        }

    private val findAllByNameCache =
        MapCache<CountryCodeNamePaginationKeyCache, PageableDto<AnimeDto>>(
            "AnimeCacheService.findAllByNameCache",
            classes = listOf(Anime::class.java)
        ) {
            try {
                PageableDto.fromPageable(
                    animeService.findAllByName(it.countryCode, it.name, it.page, it.limit, it.searchTypes),
                    AnimeDto::class.java
                )
            } catch (e: Exception) {
                e.printStackTrace()
                PageableDto.empty()
            }
        }

    private val findAllByCountryCodeCache = MapCache<CountryCode, List<Anime>>(
        "AnimeCacheService.findAllByCountryCodeCache",
        classes = listOf(Anime::class.java, EpisodeMapping::class.java),
        fn = { CountryCode.entries },
        requiredCaches = { listOf(episodeVariantCacheService.findAllCache) }
    ) {
        (episodeVariantCacheService.findAllCache[DEFAULT_ALL_KEY] ?: emptyList()).asSequence()
            .map { it.mapping!!.anime!! }
            .distinctBy { it.uuid }
            .filter { anime -> anime.countryCode == it }
            .toList()
    }

    private val weeklyMemberCache =
        MapCache<CountryCodeLocalDateKeyCache, List<WeeklyAnimesDto>>(
            "AnimeCacheService.weeklyMemberCache",
            classes = listOf(
                Anime::class.java,
                EpisodeMapping::class.java,
                EpisodeVariant::class.java,
                MemberFollowAnime::class.java
            )
        ) {
            animeService.getWeeklyAnimes(
                it.countryCode,
                it.member?.let { uuid -> memberCacheService.find(uuid) },
                it.localDate
            )
        }

    private val weeklyMemberV2Cache =
        MapCache<CountryCodeLocalDateKeyCache, List<fr.shikkanime.dtos.weekly.v2.WeeklyAnimesDto>>(
            "AnimeCacheService.weeklyMemberV2Cache",
            classes = listOf(
                Anime::class.java,
                EpisodeMapping::class.java,
                EpisodeVariant::class.java,
                MemberFollowAnime::class.java
            )
        ) {
            try {
                animeService.getWeeklyAnimesV2(
                    it.countryCode,
                    it.member?.let { uuid -> memberCacheService.find(uuid) },
                    it.localDate
                )
            } catch (e: Exception) {
                e.printStackTrace()
                emptyList()
            }
        }

    val findAllAudioLocalesAndSeasonsCache = MapCache(
        "AnimeCacheService.findAllAudioLocalesAndSeasonsCache",
        classes = listOf(
            Anime::class.java,
            EpisodeMapping::class.java,
            EpisodeVariant::class.java
        ),
        fn = { listOf(DEFAULT_ALL_KEY) },
        requiredCaches = { listOf(episodeVariantCacheService.findAllCache) }
    ) {
        (episodeVariantCacheService.findAllCache[DEFAULT_ALL_KEY] ?: emptyList())
            .groupBy { it.mapping!!.anime!!.uuid!! }
            .mapValues { entry ->
                val audioLocales = entry.value.map { it.audioLocale!! }.toSet()
                val seasons = entry.value.groupBy { it.mapping!!.season!! }
                    .mapValues { it.value.maxOfOrNull { variant -> variant.releaseDateTime }!! }
                    .toSortedMap()

                audioLocales to seasons
            }
    }

    fun findAllBy(
        countryCode: CountryCode?,
        uuid: UUID?,
        sort: List<SortParameter>,
        page: Int,
        limit: Int,
        searchTypes: Array<LangType>? = null,
        status: Status? = null,
    ) = findAllByCache[CountryCodeUUIDSortPaginationKeyCache(countryCode, uuid, sort, page, limit, searchTypes, status)]

    fun findAllByName(countryCode: CountryCode?, name: String, page: Int, limit: Int, searchTypes: Array<LangType>?) =
        findAllByNameCache[CountryCodeNamePaginationKeyCache(countryCode, name, page, limit, searchTypes)]

    fun findAll() = CountryCode.entries.flatMap { findAllByCountryCodeCache[it]!! }

    fun find(uuid: UUID?) = findAll().find { it.uuid == uuid }

    fun findBySlug(countryCode: CountryCode, slug: String) = findAllByCountryCodeCache[countryCode]!!.find { it.slug == slug }

    fun getWeeklyAnimes(countryCode: CountryCode, memberUuid: UUID?, startOfWeekDay: LocalDate) =
        weeklyMemberCache[CountryCodeLocalDateKeyCache(memberUuid, countryCode, startOfWeekDay)]

    fun getWeeklyAnimesV2(countryCode: CountryCode, memberUuid: UUID?, startOfWeekDay: LocalDate) =
        weeklyMemberV2Cache[CountryCodeLocalDateKeyCache(memberUuid, countryCode, startOfWeekDay)]

    fun findAudioLocalesAndSeasonsByAnimeCache(anime: Anime) =
        findAllAudioLocalesAndSeasonsCache[DEFAULT_ALL_KEY]?.get(anime.uuid!!)
}