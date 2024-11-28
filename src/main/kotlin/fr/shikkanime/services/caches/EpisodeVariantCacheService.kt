package fr.shikkanime.services.caches

import com.google.inject.Inject
import fr.shikkanime.converters.AbstractConverter
import fr.shikkanime.dtos.variants.EpisodeVariantDto
import fr.shikkanime.dtos.variants.VariantReleaseDto
import fr.shikkanime.entities.Anime
import fr.shikkanime.entities.EpisodeMapping
import fr.shikkanime.entities.EpisodeVariant
import fr.shikkanime.entities.Member
import fr.shikkanime.entities.enums.CountryCode
import fr.shikkanime.entities.enums.EpisodeType
import fr.shikkanime.services.EpisodeVariantService
import fr.shikkanime.services.MemberFollowAnimeService
import fr.shikkanime.services.MemberService
import fr.shikkanime.utils.MapCache
import fr.shikkanime.utils.atEndOfTheDay
import fr.shikkanime.utils.atEndOfWeek
import fr.shikkanime.utils.isBetween
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID
import kotlin.collections.get

class EpisodeVariantCacheService : AbstractCacheService {
    companion object {
        private const val DEFAULT_ALL_KEY = "all"
    }

    @Inject
    private lateinit var episodeVariantService: EpisodeVariantService

    @Inject
    private lateinit var memberService: MemberService

    @Inject
    private lateinit var memberFollowAnimeService: MemberFollowAnimeService

    private val findAllCache = MapCache(
        classes = listOf(EpisodeMapping::class.java, EpisodeVariant::class.java),
        fn = { listOf(DEFAULT_ALL_KEY) }
    ) {
        episodeVariantService.findAll().associateBy { it.uuid!! }
    }

    private val findAllByEpisodeMappingCache = MapCache(
        classes = listOf(EpisodeMapping::class.java, EpisodeVariant::class.java),
        fn = { listOf(DEFAULT_ALL_KEY) }
    ) {
        (findAllCache[DEFAULT_ALL_KEY]?.values ?: emptyList()).asSequence()
            .sortedBy { it.releaseDateTime }
            .groupBy { it.mapping!!.uuid!! }
            .mapValues { entry -> entry.value.toSet() }
    }

    private val findAllSimulcastedByAnimeCache = MapCache(
        classes = listOf(EpisodeMapping::class.java, EpisodeVariant::class.java),
        fn = { CountryCode.entries }
    ) {
        (findAllCache[DEFAULT_ALL_KEY]?.values ?: emptyList()).asSequence()
            .filter { variant ->
                variant.audioLocale != it.locale &&
                        variant.mapping!!.episodeType != EpisodeType.FILM &&
                        variant.mapping!!.episodeType != EpisodeType.SUMMARY
            }.sortedWith(compareBy({ it.mapping!!.releaseDateTime }, { it.mapping!!.season }, { it.mapping!!.episodeType }, { it.mapping!!.number }))
            .groupBy { it.mapping!!.anime!!.uuid!! }
            .mapValues { entry -> entry.value }
    }

    private val findAllAnimeEpisodeMappingReleaseDateTimePlatformAudioLocaleCache = MapCache<Pair<CountryCode, Member?>, List<VariantReleaseDto>>(
        classes = listOf(EpisodeVariant::class.java),
        fn = {
            memberService.findAll()
                .filter(Member::isPrivate)
                .flatMap { member -> CountryCode.entries.map { countryCode -> countryCode to member } }
        }
    ) { pair ->
        val followedAnimes = pair.second?.let { member -> memberFollowAnimeService.findAllFollowedAnimesUUID(member) } ?: emptySet()

        (findAllCache[DEFAULT_ALL_KEY]?.values ?: emptyList()).asSequence()
            .filter { variant -> variant.mapping!!.anime!!.countryCode == pair.first && (pair.second == null || variant.mapping!!.anime!!.uuid!! in followedAnimes) }
            .sortedWith(compareBy({ it.releaseDateTime }, { it.mapping!!.season }, { it.mapping!!.episodeType }, { it.mapping!!.number }))
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

    fun find(uuid: UUID) = findAllCache[DEFAULT_ALL_KEY]?.get(uuid)?.let {
        AbstractConverter.convert(it, EpisodeVariantDto::class.java)
    }

    fun findAllByMapping(episodeMapping: EpisodeMapping) =
        findAllByEpisodeMappingCache[DEFAULT_ALL_KEY]?.get(episodeMapping.uuid)

    fun findAllSimulcastedByAnime(anime: Anime) =
        findAllSimulcastedByAnimeCache[anime.countryCode!!]?.get(anime.uuid!!)

    fun findAllAnimeEpisodeMappingReleaseDateTimePlatformAudioLocale(
        countryCode: CountryCode,
        member: Member?,
        startOfWeekDay: LocalDate,
        zoneId: ZoneId,
    ): List<VariantReleaseDto> {
        val startOfPreviousWeek = startOfWeekDay.minusWeeks(1).atStartOfDay(zoneId)
        val endOfWeek = startOfWeekDay.atEndOfWeek().atEndOfTheDay(zoneId)

        return findAllAnimeEpisodeMappingReleaseDateTimePlatformAudioLocaleCache[countryCode to member]
            ?.filter { variantReleaseDto -> variantReleaseDto.releaseDateTime.isBetween(startOfPreviousWeek, endOfWeek) }
            ?: emptyList()
    }
}