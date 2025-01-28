package fr.shikkanime.services.caches

import com.google.inject.Inject
import fr.shikkanime.caches.CountryCodeMemberUUIDWeekKeyCache
import fr.shikkanime.converters.AbstractConverter
import fr.shikkanime.dtos.variants.EpisodeVariantDto
import fr.shikkanime.entities.EpisodeMapping
import fr.shikkanime.entities.EpisodeVariant
import fr.shikkanime.entities.Member
import fr.shikkanime.entities.MemberFollowAnime
import fr.shikkanime.entities.enums.CountryCode
import fr.shikkanime.services.EpisodeVariantService
import fr.shikkanime.utils.MapCache
import fr.shikkanime.utils.atEndOfTheDay
import fr.shikkanime.utils.atEndOfWeek
import java.time.LocalDate
import java.time.ZoneId
import java.util.*

class EpisodeVariantCacheService : AbstractCacheService {
    @Inject
    private lateinit var episodeVariantService: EpisodeVariantService

    @Inject
    private lateinit var memberCacheService: MemberCacheService

    fun findAllByMapping(episodeMapping: EpisodeMapping) = MapCache.getOrCompute(
        "EpisodeVariantCacheService.findAllByMapping",
        classes = listOf(EpisodeMapping::class.java, EpisodeVariant::class.java),
        key = episodeMapping.uuid!!,
    ) { episodeVariantService.findAllByMapping(it) }.toSet()

    fun findAllVariantReleases(
        countryCode: CountryCode,
        member: Member?,
        startOfWeekDay: LocalDate,
        zoneId: ZoneId,
    ) = MapCache.getOrCompute(
        "EpisodeVariantCacheService.findAllVariantReleases",
        classes = listOf(EpisodeVariant::class.java, MemberFollowAnime::class.java),
        key = CountryCodeMemberUUIDWeekKeyCache(countryCode, member?.uuid, startOfWeekDay.minusWeeks(1).atStartOfDay(zoneId), startOfWeekDay.atEndOfWeek().atEndOfTheDay(zoneId)),
    ) {
        episodeVariantService.findAllVariantReleases(
            it.countryCode,
            it.member?.let { uuid -> memberCacheService.find(uuid) },
            it.startZonedDateTime,
            it.endZonedDateTime
        )
    }

    fun find(uuid: UUID) = MapCache.getOrCompute(
        "EpisodeVariantCacheService.find",
        classes = listOf(EpisodeVariant::class.java),
        key = uuid,
    ) { episodeVariantService.find(it)?.let { AbstractConverter.convert(it, EpisodeVariantDto::class.java) } }
}