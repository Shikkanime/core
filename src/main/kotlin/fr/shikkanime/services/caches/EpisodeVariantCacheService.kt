package fr.shikkanime.services.caches

import com.google.gson.reflect.TypeToken
import com.google.inject.Inject
import fr.shikkanime.caches.CountryCodePlatformWeekKeyCache
import fr.shikkanime.dtos.variants.EpisodeVariantDto
import fr.shikkanime.entities.EpisodeMapping
import fr.shikkanime.entities.EpisodeVariant
import fr.shikkanime.entities.enums.CountryCode
import fr.shikkanime.entities.enums.Platform
import fr.shikkanime.factories.impl.EpisodeVariantFactory
import fr.shikkanime.services.EpisodeVariantService
import fr.shikkanime.utils.MapCache
import fr.shikkanime.utils.MapCacheValue
import fr.shikkanime.utils.SerializationUtils
import java.time.ZonedDateTime
import java.util.*

class EpisodeVariantCacheService : ICacheService {
    @Inject private lateinit var episodeVariantService: EpisodeVariantService
    @Inject private lateinit var episodeVariantFactory: EpisodeVariantFactory

    fun findAllByMapping(episodeMapping: EpisodeMapping) = MapCache.getOrCompute(
        "EpisodeVariantCacheService.findAllByMapping",
        classes = listOf(EpisodeMapping::class.java, EpisodeVariant::class.java),
        typeToken = object : TypeToken<MapCacheValue<Array<EpisodeVariantDto>>>() {},
        serializationType = SerializationUtils.SerializationType.JSON,
        key = episodeMapping.uuid!!,
    ) { uuid -> episodeVariantService.findAllByMapping(uuid).map { episodeVariantFactory.toDto(it, false) }.toTypedArray() }

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

    fun find(uuid: UUID) = MapCache.getOrComputeNullable(
        "EpisodeVariantCacheService.find",
        classes = listOf(EpisodeVariant::class.java),
        typeToken = object : TypeToken<MapCacheValue<EpisodeVariantDto>>() {},
        key = uuid,
    ) { episodeVariantService.find(it)?.let(episodeVariantFactory::toDto) }
}