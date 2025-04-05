package fr.shikkanime.services.caches

import com.google.inject.Inject
import fr.shikkanime.caches.CountryCodeMemberUUIDWeekKeyCache
import fr.shikkanime.caches.CountryCodePlatformWeekKeyCache
import fr.shikkanime.entities.EpisodeMapping
import fr.shikkanime.entities.EpisodeVariant
import fr.shikkanime.entities.Member
import fr.shikkanime.entities.MemberFollowAnime
import fr.shikkanime.entities.enums.CountryCode
import fr.shikkanime.entities.enums.Platform
import fr.shikkanime.factories.impl.EpisodeVariantFactory
import fr.shikkanime.services.EpisodeVariantService
import fr.shikkanime.utils.MapCache
import fr.shikkanime.utils.atEndOfTheDay
import fr.shikkanime.utils.atEndOfWeek
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.*

class EpisodeVariantCacheService : ICacheService {
    @Inject
    private lateinit var episodeVariantService: EpisodeVariantService

    @Inject
    private lateinit var memberCacheService: MemberCacheService

    @Inject
    private lateinit var episodeVariantFactory: EpisodeVariantFactory

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

    fun findAllVariantsByCountryCodeAndPlatformAndReleaseDateTimeBetween(
        countryCode: CountryCode,
        platform: Platform,
        startZonedDateTime: ZonedDateTime,
        endZonedDateTime: ZonedDateTime,
    ) = MapCache.getOrCompute(
        "EpisodeVariantCacheService.findAllVariantsByCountryCodeAndPlatformAndReleaseDateTimeBetween",
        classes = listOf(EpisodeVariant::class.java),
        key = CountryCodePlatformWeekKeyCache(countryCode, platform, startZonedDateTime, endZonedDateTime),
    ) {
        episodeVariantService.findAllVariantsByCountryCodeAndPlatformAndReleaseDateTimeBetween(
            it.countryCode,
            it.platform,
            it.startZonedDateTime,
            it.endZonedDateTime
        )
    }

    fun findAllIdentifiers() = MapCache.getOrCompute(
        "EpisodeVariantCacheService.findAllIdentifiers",
        classes = listOf(EpisodeVariant::class.java),
        key = "",
    ) { episodeVariantService.findAllIdentifiers() }

    fun find(uuid: UUID) = MapCache.getOrComputeNullable(
        "EpisodeVariantCacheService.find",
        classes = listOf(EpisodeVariant::class.java),
        key = uuid,
    ) { episodeVariantService.find(it)?.let { episodeVariantFactory.toDto(it) } }
}