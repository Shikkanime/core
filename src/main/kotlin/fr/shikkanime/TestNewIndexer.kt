package fr.shikkanime

import fr.shikkanime.entities.EpisodeVariant
import fr.shikkanime.entities.enums.EpisodeType
import fr.shikkanime.entities.enums.LangType
import fr.shikkanime.services.EpisodeVariantService
import fr.shikkanime.utils.Constant
import java.time.ZonedDateTime
import java.util.*
import kotlin.math.abs
import kotlin.system.exitProcess

private const val TIME_WINDOW_HOURS = 2L

private data class TimeCompositeIndex(
    val releaseDateTime: ZonedDateTime,
    val animeUuid: UUID,
    val animeSlug: String,
    val mappingUuid: UUID,
    val episodeType: EpisodeType,
): Comparable<TimeCompositeIndex> {
    override fun compareTo(other: TimeCompositeIndex) = compareValuesBy<TimeCompositeIndex>(
        this, other,
        { it.animeUuid },
        { it.episodeType },
        { it.releaseDateTime },
        { it.mappingUuid },
    )

    fun toTimeRange(): Pair<TimeCompositeIndex, TimeCompositeIndex> {
        val start = copy(releaseDateTime = releaseDateTime.minusHours(TIME_WINDOW_HOURS))
        val end = copy(releaseDateTime = releaseDateTime.plusHours(TIME_WINDOW_HOURS))
        return start to end
    }
}

private fun EpisodeVariant.hasSameMapping(other: EpisodeVariant): Boolean =
    mapping!!.uuid == other.mapping!!.uuid

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

    val index = TreeMap<TimeCompositeIndex, MutableSet<EpisodeVariant>>()

    variants.forEach { variant ->
        val composite = TimeCompositeIndex(
            releaseDateTime = variant.releaseDateTime,
            animeUuid = variant.mapping!!.anime!!.uuid!!,
            animeSlug = variant.mapping!!.anime!!.slug!!,
            mappingUuid = variant.mapping!!.uuid!!,
            episodeType = variant.mapping!!.episodeType!!
        )

        val (start, end) = composite.toTimeRange()

        val existingEntry = index.subMap(start, true, end, true)
            .entries
            .firstOrNull { it.value.shouldGroupWith(variant) }

        if (existingEntry != null) {
            existingEntry.value.add(variant)
        } else {
            index[composite] = mutableSetOf(variant)
        }
    }

    // Get the last 10 entries in the compositeIndexMap
    val lastEntries = index.toSortedMap().entries
        .sortedWith(
            compareByDescending<Map.Entry<TimeCompositeIndex, MutableSet<EpisodeVariant>>> { it.key.releaseDateTime }
                .thenByDescending { it.key.animeSlug }
                .thenByDescending { it.key.episodeType }
        )
        .take(500)

    println("Last 10 entries in compositeIndexMap:")
    lastEntries.forEach { (composite, uuids) ->
        println("Composite: $composite, UUIDs: $uuids")
    }

    exitProcess(0)
}