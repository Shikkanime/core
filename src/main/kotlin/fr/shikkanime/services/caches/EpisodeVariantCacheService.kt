package fr.shikkanime.services.caches

import com.google.inject.Inject
import fr.shikkanime.caches.CountryCodeMemberUUIDWeekKeyCache
import fr.shikkanime.caches.CountryCodePlatformWeekKeyCache
import fr.shikkanime.entities.EpisodeMapping
import fr.shikkanime.entities.EpisodeVariant
import fr.shikkanime.entities.Member
import fr.shikkanime.entities.MemberFollowAnime
import fr.shikkanime.entities.enums.CountryCode
import fr.shikkanime.entities.enums.LangType
import fr.shikkanime.entities.enums.Platform
import fr.shikkanime.factories.impl.EpisodeVariantFactory
import fr.shikkanime.services.EpisodeVariantService
import fr.shikkanime.utils.*
import fr.shikkanime.utils.TelemetryConfig.trace
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.*

class EpisodeVariantCacheService : ICacheService {
    private val tracer = TelemetryConfig.getTracer("EpisodeVariantCacheService")
    @Inject private lateinit var episodeVariantService: EpisodeVariantService
    @Inject private lateinit var memberCacheService: MemberCacheService
    @Inject private lateinit var episodeVariantFactory: EpisodeVariantFactory
    
    private val byMappingCache = MapCache<UUID, Set<EpisodeVariant>>(
        "EpisodeVariantCacheService.findAllByMapping",
        classes = listOf(EpisodeMapping::class.java, EpisodeVariant::class.java),
    ) { tracer.trace { episodeVariantService.findAllByMapping(it).toSet() } }

    fun findAllByMapping(episodeMapping: EpisodeMapping) = byMappingCache[episodeMapping.uuid!!] ?: emptySet()

    fun findAllByMappings(vararg episodeMappings: EpisodeMapping) {
        val notFetched = episodeMappings.filterNot { byMappingCache.containsKeyAndValid(it.uuid!!) }

        if (notFetched.isNotEmpty()) {
            episodeVariantService.findAllByMappings(*notFetched.mapNotNull { it.uuid }.toTypedArray())
                .groupBy { it.mapping!!.uuid!! }
                .forEach { byMappingCache.setIfNotExists(it.key, it.value.toSet()) }
        }

        episodeMappings.forEach { byMappingCache[it.uuid!!]!! }
    }

    fun findAllVariantReleases(
        countryCode: CountryCode,
        member: Member?,
        startOfWeekDay: LocalDate,
        zoneId: ZoneId,
        searchTypes: Array<LangType>? = null,
    ) = MapCache.getOrCompute(
        "EpisodeVariantCacheService.findAllVariantReleases",
        classes = listOf(EpisodeVariant::class.java, MemberFollowAnime::class.java),
        key = CountryCodeMemberUUIDWeekKeyCache(countryCode, member?.uuid, startOfWeekDay.minusWeeks(1).atStartOfDay(zoneId), startOfWeekDay.atEndOfWeek().atEndOfTheDay(zoneId), searchTypes),
    ) { tracer.trace {
        episodeVariantService.findAllVariantReleases(
            it.countryCode,
            it.member?.let { uuid -> memberCacheService.find(uuid) },
            it.startZonedDateTime,
            it.endZonedDateTime,
            it.searchTypes
        )
    } }

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
        key = StringUtils.EMPTY_STRING,
    ) { episodeVariantService.findAllIdentifiers() }

    fun find(uuid: UUID) = MapCache.getOrComputeNullable(
        "EpisodeVariantCacheService.find",
        classes = listOf(EpisodeVariant::class.java),
        key = uuid,
    ) { episodeVariantService.find(it)?.let { episodeVariantFactory.toDto(it) } }

    fun findByIdentifier(identifier: String) = MapCache.getOrComputeNullable(
        "EpisodeVariantCacheService.findByIdentifier",
        classes = listOf(EpisodeVariant::class.java),
        key = identifier,
    ) { episodeVariantService.findByIdentifier(it) }
}