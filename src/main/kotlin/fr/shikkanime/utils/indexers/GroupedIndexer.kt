package fr.shikkanime.utils.indexers

import fr.shikkanime.entities.enums.CountryCode
import fr.shikkanime.entities.enums.EpisodeType
import fr.shikkanime.entities.miscellaneous.Pageable
import fr.shikkanime.utils.isAfterOrEqual
import fr.shikkanime.utils.isBeforeOrEqual
import java.time.ZonedDateTime
import java.util.*

object GroupedIndexer {
    data class CompositeIndex(
        val countryCode: CountryCode,
        val animeSlug: String,
        val episodeType: EpisodeType
    )

    data class IndexedData(
        val releaseDateTime: ZonedDateTime,
        val variantsUuid: Set<UUID>
    )

    fun isTimeInRange(timeToCheck: ZonedDateTime, referenceTime: ZonedDateTime, toleranceHours: Long): Boolean {
        val minTime = referenceTime.minusHours(toleranceHours)
        val maxTime = referenceTime.plusHours(toleranceHours)

        return timeToCheck.isAfterOrEqual(minTime) && timeToCheck.isBeforeOrEqual(maxTime)
    }

    private val index = HashMap<CompositeIndex, Set<IndexedData>>()

    fun clear() {
        index.clear()
    }

    fun add(
        countryCode: CountryCode,
        animeSlug: String,
        episodeType: EpisodeType,
        releaseDateTime: ZonedDateTime,
        variantUuid: UUID
    ) {
        val key = CompositeIndex(countryCode, animeSlug, episodeType)
        val existingData = index[key].orEmpty()
        val indexedData = existingData.find { isTimeInRange(releaseDateTime, it.releaseDateTime, 2) }
        val updatedData = indexedData?.copy(variantsUuid = indexedData.variantsUuid + variantUuid) ?: IndexedData(releaseDateTime, setOf(variantUuid))
        index[key] = (if (indexedData != null) existingData - indexedData else existingData) + updatedData
    }

    fun pageable(
        page: Int,
        limit: Int,
        filter: ((Map.Entry<CompositeIndex, Set<IndexedData>>) -> Boolean)? = null,
        comparator: Comparator<Pair<IndexedData, CompositeIndex>>? = null
    ): Pageable<Pair<IndexedData, CompositeIndex>> {
        val filteredIndex = index.asSequence().let { if (filter != null) it.filter(filter) else it }
        val sortedIndex = filteredIndex
            .flatMap { entry -> entry.value.map { data -> data to entry.key } }
            .let { if (comparator != null) it.sortedWith(comparator) else it }
        val total = sortedIndex.count().toLong()
        val pagedIndex = sortedIndex.drop((page - 1) * limit).take(limit).toSet()
        return Pageable(pagedIndex, page, limit, total)
    }
}