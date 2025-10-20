package fr.shikkanime.utils.indexers

import fr.shikkanime.entities.enums.CountryCode
import fr.shikkanime.entities.enums.EpisodeType
import fr.shikkanime.entities.miscellaneous.Pageable
import java.time.ZonedDateTime
import java.util.*

private typealias IndexedData = TreeMap<ZonedDateTime, MutableSet<UUID>>
private typealias IndexedDataEntry = Map.Entry<ZonedDateTime, MutableSet<UUID>>
typealias ReverseIndexedRecord = Pair<IndexedDataEntry, GroupedIndexer.CompositeIndex>

object GroupedIndexer {
    data class CompositeIndex(
        val countryCode: CountryCode,
        val animeSlug: String,
        val episodeType: EpisodeType
    )

    private val index = HashMap<CompositeIndex, IndexedData>()

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
        val existingData = index.getOrPut(key) { TreeMap() }
        val matchingMin = releaseDateTime.minusHours(2)
        val matchingMax = releaseDateTime.plusHours(2)
        val indexedData = existingData.subMap(matchingMin, true, matchingMax, true).firstEntry()
        indexedData?.let { existingData.remove(it.key, it.value) }
        val updatedData = indexedData?.apply { value.add(variantUuid) } ?: AbstractMap.SimpleEntry(releaseDateTime, mutableSetOf(variantUuid))
        existingData[updatedData.key] = updatedData.value
    }

    fun pageable(
        page: Int,
        limit: Int,
        filter: ((Map.Entry<CompositeIndex, IndexedData>) -> Boolean)? = null,
        comparator: Comparator<ReverseIndexedRecord>? = null
    ): Pageable<ReverseIndexedRecord> {
        val filteredIndex = index.asSequence().let { if (filter != null) it.filter(filter) else it }
        val sortedIndex = filteredIndex
            .flatMap { entry -> entry.value.asSequence().map { data -> data to entry.key } }
            .let { if (comparator != null) it.sortedWith(comparator) else it }
        val total = sortedIndex.count().toLong()
        val pagedIndex = sortedIndex.drop((page - 1).coerceAtLeast(0) * limit).take(limit).toSet()
        return Pageable(pagedIndex, page, limit, total)
    }
}