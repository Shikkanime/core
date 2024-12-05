package fr.shikkanime.services.caches

import com.google.inject.Inject
import fr.shikkanime.converters.AbstractConverter
import fr.shikkanime.dtos.variants.EpisodeVariantDto
import fr.shikkanime.dtos.variants.VariantReleaseDto
import fr.shikkanime.entities.EpisodeMapping
import fr.shikkanime.entities.EpisodeVariant
import fr.shikkanime.entities.Member
import fr.shikkanime.entities.MemberFollowAnime
import fr.shikkanime.entities.enums.CountryCode
import fr.shikkanime.services.EpisodeVariantService
import fr.shikkanime.services.MemberFollowAnimeService
import fr.shikkanime.utils.MapCache
import fr.shikkanime.utils.atEndOfTheDay
import fr.shikkanime.utils.atEndOfWeek
import fr.shikkanime.utils.isBetween
import java.time.LocalDate
import java.time.ZoneId
import java.util.*

class EpisodeVariantCacheService : AbstractCacheService {
    companion object {
        private const val DEFAULT_ALL_KEY = "all"
    }

    @Inject
    private lateinit var episodeVariantService: EpisodeVariantService

    @Inject
    private lateinit var memberFollowAnimeService: MemberFollowAnimeService

    val findAllCache = MapCache(
        "EpisodeVariantCacheService.findAllCache",
        classes = listOf(EpisodeMapping::class.java, EpisodeVariant::class.java),
        fn = { listOf(DEFAULT_ALL_KEY) }
    ) {
        episodeVariantService.findAll()
    }

    val findAllByEpisodeMappingCache = MapCache(
        "EpisodeVariantCacheService.findAllByEpisodeMappingCache",
        classes = listOf(EpisodeMapping::class.java, EpisodeVariant::class.java),
        fn = { listOf(DEFAULT_ALL_KEY) },
        requiredCaches = { listOf(findAllCache) }
    ) {
        (findAllCache[DEFAULT_ALL_KEY] ?: emptyList()).asSequence()
            .sortedBy { it.releaseDateTime }
            .groupBy { it.mapping!!.uuid!! }
            .mapValues { entry -> entry.value.toSet() }
    }

    private val findAllAnimeEpisodeMappingReleaseDateTimePlatformAudioLocaleCache =
        MapCache<CountryCode, List<VariantReleaseDto>>(
            "EpisodeVariantCacheService.findAllAnimeEpisodeMappingReleaseDateTimePlatformAudioLocaleCache",
            classes = listOf(EpisodeVariant::class.java),
            fn = { CountryCode.entries },
            requiredCaches = { listOf(findAllCache) }
        ) {
            (findAllCache[DEFAULT_ALL_KEY] ?: emptyList()).asSequence()
                .filter { variant -> variant.mapping!!.anime!!.countryCode == it }
                .sortedWith(
                    compareBy(
                        { it.releaseDateTime },
                        { it.mapping!!.season },
                        { it.mapping!!.episodeType },
                        { it.mapping!!.number })
                )
                .map { variant ->
                    VariantReleaseDto(
                        anime = variant.mapping!!.anime!!,
                        episodeMapping = variant.mapping!!,
                        releaseDateTime = variant.releaseDateTime,
                        platform = variant.platform!!,
                        audioLocale = variant.audioLocale!!,
                    )
                }.toList()
        }

    private val findAllAnimeEpisodeMappingReleaseDateTimePlatformAudioLocaleWithMemberCache =
        MapCache<Member, List<VariantReleaseDto>>(
            "EpisodeVariantCacheService.findAllAnimeEpisodeMappingReleaseDateTimePlatformAudioLocaleWithMemberCache",
            classes = listOf(EpisodeVariant::class.java, MemberFollowAnime::class.java),
            requiredCaches = { listOf(findAllCache) }
        ) {
            val followedAnimes = memberFollowAnimeService.findAllFollowedAnimesUUID(it)

            (findAllCache[DEFAULT_ALL_KEY] ?: emptyList()).asSequence()
                .filter { variant -> variant.mapping!!.anime!!.uuid!! in followedAnimes }
                .sortedWith(
                    compareBy(
                        { it.releaseDateTime },
                        { it.mapping!!.season },
                        { it.mapping!!.episodeType },
                        { it.mapping!!.number })
                )
                .map { variant ->
                    VariantReleaseDto(
                        anime = variant.mapping!!.anime!!,
                        episodeMapping = variant.mapping!!,
                        releaseDateTime = variant.releaseDateTime,
                        platform = variant.platform!!,
                        audioLocale = variant.audioLocale!!,
                    )
                }.toList()
        }

    private val findAllIdentifiersCache = MapCache(
        "EpisodeVariantCacheService.findAllIdentifiersCache",
        classes = listOf(EpisodeVariant::class.java),
        fn = { listOf(DEFAULT_ALL_KEY) },
        requiredCaches = { listOf(findAllCache) }
    ) {
        (findAllCache[DEFAULT_ALL_KEY] ?: emptyList()).map { it.identifier!! }.toSet()
    }

    fun findAll() = findAllCache[DEFAULT_ALL_KEY] ?: emptyList()

    fun find(uuid: UUID) = findAllCache[DEFAULT_ALL_KEY]?.find { it.uuid == uuid }?.let {
        AbstractConverter.convert(it, EpisodeVariantDto::class.java)
    }

    fun findAllByMapping(episodeMapping: EpisodeMapping) =
        findAllByEpisodeMappingCache[DEFAULT_ALL_KEY]?.get(episodeMapping.uuid)

    fun findAllAnimeEpisodeMappingReleaseDateTimePlatformAudioLocale(
        countryCode: CountryCode,
        member: Member?,
        startOfWeekDay: LocalDate,
        zoneId: ZoneId,
    ): List<VariantReleaseDto> {
        val startOfPreviousWeek = startOfWeekDay.minusWeeks(1).atStartOfDay(zoneId)
        val endOfWeek = startOfWeekDay.atEndOfWeek().atEndOfTheDay(zoneId)

        val values = if (member != null) {
            findAllAnimeEpisodeMappingReleaseDateTimePlatformAudioLocaleWithMemberCache[member]
        } else {
            findAllAnimeEpisodeMappingReleaseDateTimePlatformAudioLocaleCache[countryCode]
        }

        return values?.filter { variantReleaseDto ->
            variantReleaseDto.releaseDateTime.isBetween(
                startOfPreviousWeek,
                endOfWeek
            )
        } ?: emptyList()
    }

    fun findAllIdentifiers() = findAllIdentifiersCache[DEFAULT_ALL_KEY] ?: emptySet()
}