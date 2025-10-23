package fr.shikkanime.utils.indexers

import fr.shikkanime.entities.enums.CountryCode
import fr.shikkanime.entities.enums.EpisodeType
import fr.shikkanime.entities.enums.LangType
import fr.shikkanime.entities.miscellaneous.Pageable
import fr.shikkanime.utils.indexers.GroupedIndexer.Data
import java.time.ZonedDateTime
import java.util.*

private typealias IndexedData = TreeMap<ZonedDateTime, MutableSet<Data>>
private typealias IndexedDataEntry = Map.Entry<ZonedDateTime, MutableSet<Data>>
typealias ReverseIndexedRecord = Pair<IndexedDataEntry, GroupedIndexer.CompositeIndex>
typealias ReverseDataIndexRecord = Triple<Data, IndexedDataEntry, GroupedIndexer.CompositeIndex>

object GroupedIndexer {
    data class CompositeIndex(
        val countryCode: CountryCode,
        val animeUuid: UUID,
        val animeSlug: String,
        val episodeType: EpisodeType
    )

    data class Data(
        val uuid: UUID,
        val audioLocale: String
    )

    val index = HashMap<CompositeIndex, IndexedData>()

    fun clear() {
        index.clear()
    }

    fun add(
        countryCode: CountryCode,
        animeUuid: UUID,
        animeSlug: String,
        episodeType: EpisodeType,
        releaseDateTime: ZonedDateTime,
        variantUuid: UUID,
        audioLocale: String
    ) {
        val key = CompositeIndex(countryCode, animeUuid, animeSlug, episodeType)
        val existingData = index.getOrPut(key) { TreeMap() }
        val matchingMin = releaseDateTime.minusHours(2)
        val matchingMax = releaseDateTime.plusHours(2)
        val indexedData: IndexedDataEntry? = existingData.subMap(matchingMin, true, matchingMax, true).firstEntry()

        // If the lang type of the audioLocale is SUBTITLES and it's already present in the indexed data and if audioLocale is 'en-US', skip it
        val langType = LangType.fromAudioLocale(countryCode, audioLocale)
        if (indexedData != null
            && langType == LangType.SUBTITLES
            && indexedData.value.any { it.uuid != variantUuid && LangType.fromAudioLocale(countryCode, it.audioLocale) == LangType.SUBTITLES }
            && audioLocale in countryCode.excludedLocales) {
            return
        }

        indexedData?.let { existingData.remove(it.key, it.value) }
        val updatedData = indexedData?.apply { value.add(Data(variantUuid, audioLocale)) } ?: AbstractMap.SimpleEntry(releaseDateTime, mutableSetOf(Data(variantUuid, audioLocale)))
        existingData[updatedData.key] = updatedData.value
    }

    fun filterAndSortRecords(
        filter: ((ReverseIndexedRecord) -> Boolean)? = null,
        comparator: Comparator<ReverseIndexedRecord>? = null
    ): Sequence<ReverseIndexedRecord> {
        val records = index.asSequence().flatMap { (key, value) -> value.asSequence().map { it to key } }
        val filteredRecords = (filter?.let(records::filter) ?: records)
        return comparator?.let(filteredRecords::sortedWith) ?: filteredRecords
    }

    fun filterAndSortReverseDataIndexRecords(
        filter: ((ReverseDataIndexRecord) -> Boolean)? = null,
        comparator: Comparator<ReverseDataIndexRecord>? = null
    ): Sequence<ReverseDataIndexRecord> {
        val records = index.asSequence()
            .flatMap { (key, value) -> value.asSequence().flatMap { dataEntry -> dataEntry.value.asSequence().map { data -> Triple(data, dataEntry, key) } } }
        val filteredRecords = (filter?.let(records::filter) ?: records)
        return comparator?.let(filteredRecords::sortedWith) ?: filteredRecords
    }

    fun pageableRecords(
        page: Int,
        limit: Int,
        filter: ((ReverseIndexedRecord) -> Boolean)? = null,
        comparator: Comparator<ReverseIndexedRecord>? = null
    ): Pageable<ReverseIndexedRecord> {
        val sortedIndex = filterAndSortRecords(filter, comparator)
        val total = sortedIndex.count().toLong()
        val pagedIndex = sortedIndex
            .drop((page - 1).coerceAtLeast(0) * limit)
            .take(limit)
            .toSet()

        return Pageable(pagedIndex, page, limit, total)
    }
}