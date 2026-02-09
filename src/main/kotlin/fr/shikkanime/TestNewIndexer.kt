package fr.shikkanime

import fr.shikkanime.entities.EpisodeVariant
import fr.shikkanime.entities.enums.CountryCode
import fr.shikkanime.entities.enums.EpisodeType
import fr.shikkanime.entities.enums.LangType
import fr.shikkanime.services.EpisodeVariantService
import fr.shikkanime.utils.Constant
import fr.shikkanime.utils.atEndOfTheDay
import fr.shikkanime.utils.atEndOfWeek
import fr.shikkanime.utils.atStartOfWeek
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.*
import kotlin.math.abs
import kotlin.system.exitProcess

private const val TIME_WINDOW_HOURS = 2L

private data class CompositeIndex(
    val countryCode: CountryCode,
    val releaseDateTime: ZonedDateTime,
    val animeUuid: UUID?,
    val animeSlug: String?,
    val episodeType: EpisodeType?,
    val mappingUuid: UUID?
) : Comparable<CompositeIndex> {
    override fun compareTo(other: CompositeIndex) = compareValuesBy<CompositeIndex>(
        this, other,
        { it.countryCode },
        { it.releaseDateTime },
        { it.animeUuid },
        { it.episodeType },
        { it.mappingUuid }
    )

    fun toTimeRange(): Pair<CompositeIndex, CompositeIndex> =
        copy(releaseDateTime = releaseDateTime.minusHours(TIME_WINDOW_HOURS)) to
                copy(releaseDateTime = releaseDateTime.plusHours(TIME_WINDOW_HOURS))

    companion object {
        fun fromZonedDateTime(countryCode: CountryCode, releaseDateTime: ZonedDateTime) = CompositeIndex(
            countryCode = countryCode,
            releaseDateTime = releaseDateTime,
            animeUuid = null,
            animeSlug = null,
            episodeType = null,
            mappingUuid = null
        )
    }
}

private fun EpisodeVariant.hasSameMapping(other: EpisodeVariant): Boolean = mapping!!.uuid == other.mapping!!.uuid

private fun EpisodeVariant.isConsecutiveEpisodeOf(other: EpisodeVariant): Boolean {
    val thisMapping = mapping!!
    val otherMapping = other.mapping!!

    return thisMapping.anime!!.uuid == otherMapping.anime!!.uuid &&
            thisMapping.season == otherMapping.season &&
            thisMapping.episodeType == otherMapping.episodeType &&
            thisMapping.number != null && otherMapping.number != null &&
            abs(thisMapping.number!! - otherMapping.number!!) == 1
}

private fun Collection<EpisodeVariant>.shouldGroupWith(variant: EpisodeVariant): Boolean =
    any { it.hasSameMapping(variant) || it.isConsecutiveEpisodeOf(variant) }

fun main() {
    val episodeVariantService = Constant.injector.getInstance(EpisodeVariantService::class.java)
    val variants = episodeVariantService.findAll()
        .sortedWith(compareBy({ it.releaseDateTime }, { it.mapping!!.anime!!.name }, { it.mapping!!.season }, { it.mapping!!.episodeType }, { it.mapping!!.number }, { LangType.fromAudioLocale(it.mapping!!.anime!!.countryCode!!, it.audioLocale!!) }))

    val index = mutableMapOf<UUID, EpisodeVariant>()
    val timeMap = TreeMap<CompositeIndex, MutableSet<UUID>>()

    variants.forEach { variant ->
        val composite = CompositeIndex(
            countryCode = variant.mapping!!.anime!!.countryCode!!,
            releaseDateTime = variant.releaseDateTime,
            animeUuid = variant.mapping!!.anime!!.uuid!!,
            animeSlug = variant.mapping!!.anime!!.slug!!,
            episodeType = variant.mapping!!.episodeType!!,
            mappingUuid = variant.mapping!!.uuid
        )
        val (start, end) = composite.toTimeRange()

        val existingEntry = timeMap.subMap(start, true, end, true)
            .entries
            .firstOrNull { it.value.mapNotNull { uuid -> index[uuid] }.shouldGroupWith(variant) }

        if (existingEntry != null) {
            existingEntry.value.add(variant.uuid!!)
        } else {
            timeMap[composite] = mutableSetOf(variant.uuid!!)
        }

        index[variant.uuid] = variant
    }

    val zoneId = ZoneId.of(CountryCode.FR.timezone)
    val now = ZonedDateTime.now(zoneId)
    val startOfWeekDay = now.toLocalDate().atStartOfWeek()
    val startAtPreviousWeek = startOfWeekDay.minusWeeks(2).atStartOfDay(zoneId)
    val endOfCurrentWeek = startOfWeekDay.atEndOfWeek().atEndOfTheDay(zoneId)

    val lastEntries = timeMap.subMap(
        CompositeIndex.fromZonedDateTime(CountryCode.FR, startAtPreviousWeek),
        true,
        CompositeIndex.fromZonedDateTime(CountryCode.FR, endOfCurrentWeek),
        true
    ).toSortedMap(compareBy<CompositeIndex> { it.countryCode }
        .thenByDescending { it.releaseDateTime }
        .thenByDescending { it.animeSlug }
        .thenByDescending { it.episodeType }
        .thenByDescending { it.mappingUuid })

    println("Last 10 entries in compositeIndexMap:")
    lastEntries.forEach { (composite, uuids) ->
        println("Composite: $composite, UUIDs: '${uuids.joinToString("', '")}'")
    }

    exitProcess(0)
}