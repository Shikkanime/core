package fr.shikkanime.services.caches

import com.google.gson.reflect.TypeToken
import com.google.inject.Inject
import fr.shikkanime.caches.CountryCodeMemberUUIDWeekKeyCache
import fr.shikkanime.caches.CountryCodePlatformWeekKeyCache
import fr.shikkanime.dtos.variants.EpisodeVariantDto
import fr.shikkanime.dtos.variants.VariantReleaseDto
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
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.*

class EpisodeVariantCacheService : ICacheService {
    @Inject private lateinit var episodeVariantService: EpisodeVariantService
    @Inject private lateinit var memberCacheService: MemberCacheService
    @Inject private lateinit var episodeVariantFactory: EpisodeVariantFactory

    fun findAllByMapping(episodeMapping: EpisodeMapping) = MapCache.getOrCompute(
        "EpisodeVariantCacheService.findAllByMapping",
        classes = listOf(EpisodeMapping::class.java, EpisodeVariant::class.java),
        typeToken = object : TypeToken<MapCacheValue<Array<EpisodeVariantDto>>>() {},
        serializationType = SerializationUtils.SerializationType.JSON,
        key = episodeMapping.uuid!!,
    ) { uuid -> episodeVariantService.findAllByMapping(uuid).map { episodeVariantFactory.toDto(it, false) }.toTypedArray() }

    fun findAllVariantReleases(
        countryCode: CountryCode,
        member: Member?,
        startOfWeekDay: LocalDate,
        zoneId: ZoneId,
        searchTypes: Array<LangType>? = null,
    ) = MapCache.getOrCompute(
        "EpisodeVariantCacheService.findAllVariantReleases",
        classes = listOf(EpisodeVariant::class.java, MemberFollowAnime::class.java),
        typeToken = object : TypeToken<MapCacheValue<Array<VariantReleaseDto>>>() {},
        serializationType = SerializationUtils.SerializationType.OBJECT,
        key = CountryCodeMemberUUIDWeekKeyCache(countryCode, member?.uuid, startOfWeekDay.minusWeeks(1).atStartOfDay(zoneId), startOfWeekDay.atEndOfWeek().atEndOfTheDay(zoneId), searchTypes),
    ) {
        episodeVariantService.findAllVariantReleases(
            it.countryCode,
            it.member?.let { uuid -> memberCacheService.find(uuid) },
            it.startZonedDateTime,
            it.endZonedDateTime,
            it.searchTypes
        ).toTypedArray()
    }

    fun findAllVariantsByCountryCodeAndPlatformAndReleaseDateTimeBetween(
        countryCode: CountryCode,
        platform: Platform,
        startZonedDateTime: ZonedDateTime,
        endZonedDateTime: ZonedDateTime,
    ) = MapCache.getOrCompute(
        "EpisodeVariantCacheService.findAllVariantsByCountryCodeAndPlatformAndReleaseDateTimeBetween",
        classes = listOf(EpisodeVariant::class.java),
        typeToken = object : TypeToken<MapCacheValue<Array<Pair<String, ZonedDateTime>>>>() {},
        key = CountryCodePlatformWeekKeyCache(countryCode, platform, startZonedDateTime, endZonedDateTime),
    ) {
        episodeVariantService.findAllVariantsByCountryCodeAndPlatformAndReleaseDateTimeBetween(
            it.countryCode,
            it.platform,
            it.startZonedDateTime,
            it.endZonedDateTime
        ).toTypedArray()
    }

    fun findAllIdentifiers() = MapCache.getOrCompute(
        "EpisodeVariantCacheService.findAllIdentifiers",
        classes = listOf(EpisodeVariant::class.java),
        typeToken = object : TypeToken<MapCacheValue<HashSet<String>>>() {},
        key = StringUtils.EMPTY_STRING,
    ) { episodeVariantService.findAllIdentifiers() }

    fun find(uuid: UUID) = MapCache.getOrComputeNullable(
        "EpisodeVariantCacheService.find",
        classes = listOf(EpisodeVariant::class.java),
        typeToken = object : TypeToken<MapCacheValue<EpisodeVariantDto>>() {},
        key = uuid,
    ) { episodeVariantService.find(it)?.let { episodeVariantFactory.toDto(it) } }

    fun findReleaseDateTimeByIdentifier(identifier: String) = MapCache.getOrComputeNullable(
        "EpisodeVariantCacheService.findByIdentifier",
        classes = listOf(EpisodeVariant::class.java),
        typeToken = object : TypeToken<MapCacheValue<ZonedDateTime>>() {},
        key = identifier,
    ) { episodeVariantService.findReleaseDateTimeByIdentifier(it) }
}