package fr.shikkanime.utils.indexers

import fr.shikkanime.entities.enums.CountryCode
import fr.shikkanime.entities.enums.EpisodeType
import java.time.ZonedDateTime
import java.util.*

private const val TIME_WINDOW_HOURS = 2L

object GroupedIndexer {
    data class CompositeIndex(
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

    data class Data(
        val variantUuid: UUID,
        val season: Int,
        val number: Int,
        val audioLocale: String
    ) {
        fun isConsecutiveEpisode(currentIndex: CompositeIndex, otherIndex: CompositeIndex, otherData: Data): Boolean {
            return currentIndex.animeUuid == otherIndex.animeUuid &&
                    currentIndex.episodeType == otherIndex.episodeType &&
                    ((this.season == otherData.season && this.number + 1 == otherData.number) ||
                            (this.season + 1 == otherData.season && this.number == 1))
        }
    }

    private val indexMap = TreeMap<CompositeIndex, MutableSet<Data>>()

    private fun hasSameMapping(current: CompositeIndex, other: CompositeIndex) = current.mappingUuid == other.mappingUuid

    private fun Collection<Data>.shouldGroupWith(currentIndex: CompositeIndex, otherIndex: CompositeIndex, otherData: Data): Boolean =
        hasSameMapping(currentIndex, otherIndex) || any { it.isConsecutiveEpisode(currentIndex, otherIndex, otherData) }

    fun add(
        countryCode: CountryCode,
        releaseDateTime: ZonedDateTime,
        animeUuid: UUID,
        animeSlug: String,
        mappingUuid: UUID,
        season: Int,
        episodeType: EpisodeType,
        number: Int,
        variantUuid: UUID,
        audioLocale: String
    ) {
        val compositeIndex = CompositeIndex(
            countryCode = countryCode,
            releaseDateTime = releaseDateTime,
            animeUuid = animeUuid,
            animeSlug = animeSlug,
            episodeType = episodeType,
            mappingUuid = mappingUuid
        )
        val (start, end) = compositeIndex.toTimeRange()
        val data = Data(variantUuid, season, number, audioLocale)

        val matchingEntry = indexMap.subMap(start, true, end, true)
            .entries.firstOrNull { (otherCompositeIndex, values) -> values.shouldGroupWith(compositeIndex, otherCompositeIndex, data) }

        if (matchingEntry != null) {
            matchingEntry.value.add(data)
        } else {
            indexMap[compositeIndex] = mutableSetOf(data)
        }
    }

    fun filterReleasesByDateRange(
        countryCode: CountryCode,
        startReleaseDateTime: ZonedDateTime,
        endReleaseDateTime: ZonedDateTime
    ): NavigableMap<CompositeIndex, MutableSet<Data>> = indexMap.subMap(
        CompositeIndex.fromZonedDateTime(countryCode, startReleaseDateTime),
        true,
        CompositeIndex.fromZonedDateTime(countryCode, endReleaseDateTime),
        true
    )
}