package fr.shikkanime.utils.indexers

import fr.shikkanime.entities.enums.CountryCode
import fr.shikkanime.entities.enums.EpisodeType
import fr.shikkanime.entities.enums.LangType
import fr.shikkanime.entities.miscellaneous.Pageable
import fr.shikkanime.utils.indexers.GroupedIndexer.add
import fr.shikkanime.utils.indexers.GroupedIndexer.compositeIndex
import fr.shikkanime.utils.indexers.GroupedIndexer.countryIndex
import java.time.ZonedDateTime
import java.util.*

/**
 * High-performance in-memory indexer for grouped episode variants.
 *
 * It provides efficient storage and retrieval of episode data, supporting:
 * - Grouping of multiple variants (e.g., subtitles and voice) for the same episode within a 2-hour window.
 * - Fast access by anime and episode type (using [compositeIndex]).
 * - Fast access by country and date (using [countryIndex]).
 * - Filtering by language type and time range.
 */
object GroupedIndexer {
    /**
     * Unique identifier for a grouping category: country + anime + episode type.
     */
    data class CompositeKey(
        val countryCode: CountryCode,
        val animeUuid: UUID,
        val animeSlug: String,
        val episodeType: EpisodeType
    )

    /**
     * Represents a single variant (e.g., subtitles or voice) within a grouped record.
     */
    data class DataEntry(
        val uuid: UUID,
        val mappingUuid: UUID,
        val audioLocale: String,
        val langType: LangType
    )

    /**
     * Grouping of episode variants that share the same [CompositeKey] and roughly the same release time.
     */
    data class GroupedRecord(
        val key: CompositeKey,
        val releaseDateTime: ZonedDateTime,
        val releaseMillis: Long,
        val dataEntries: MutableSet<DataEntry>,
        val variants: MutableList<fr.shikkanime.entities.EpisodeVariant> = mutableListOf()
    )

    /**
     * A record representing a single [DataEntry] and its associated [GroupedRecord].
     * Useful when the application needs to treat variants as individual items while keeping the grouping context.
     */
    data class DataRecord(
        val data: DataEntry,
        val record: GroupedRecord,
        val releaseMillis: Long = record.releaseMillis
    )

    /**
     * Index by [CompositeKey] and then by [ZonedDateTime].
     * Used to find if an episode variant should be grouped with an existing one during the [add] operation.
     */
    private val compositeIndex = HashMap<CompositeKey, TreeMap<ZonedDateTime, GroupedRecord>>()

    /**
     * Index by [CountryCode] and then by [ZonedDateTime].
     * Optimized for calendar views and API listings by country, providing direct access to sorted records.
     */
    private val countryIndex =
        EnumMap<CountryCode, TreeMap<ZonedDateTime, MutableList<GroupedRecord>>>(CountryCode::class.java)

    /**
     * Clears all indexed data.
     */
    fun clear() {
        compositeIndex.clear()
        countryIndex.clear()
    }

    /**
     * Adds an episode variant to the index.
     *
     * Logic:
     * 1. Check if a [GroupedRecord] exists within a 2-hour window (+/- 2h) for the same [CompositeKey].
     * 2. If it exists, add the new [DataEntry] to that record's [dataEntries] set.
     * 3. Otherwise, create a new [GroupedRecord] and add it to both [compositeIndex] and [countryIndex].
     *
     * This grouping mechanism prevents multiple notifications or listings for the same episode released
     * in different versions (subtitles vs voice) at slightly different times.
     */
    fun add(
        key: CompositeKey,
        variantUuid: UUID,
        mappingUuid: UUID,
        releaseDateTime: ZonedDateTime,
        audioLocale: String,
        variant: fr.shikkanime.entities.EpisodeVariant? = null
    ) {
        val langType = LangType.fromAudioLocale(key.countryCode, audioLocale)
        val dataEntry = DataEntry(variantUuid, mappingUuid, audioLocale, langType)

        val animeIndex = compositeIndex.getOrPut(key) { TreeMap() }
        val matchingMin = releaseDateTime.minusHours(2)
        val matchingMax = releaseDateTime.plusHours(2)
        // Use subMap for efficient window matching in O(log N)
        val existingEntry = animeIndex.subMap(matchingMin, true, matchingMax, true).firstEntry()

        if (existingEntry != null) {
            existingEntry.value.dataEntries.add(dataEntry)
            variant?.let { existingEntry.value.variants.add(it) }
        } else {
            val record = GroupedRecord(
                key = key,
                releaseDateTime = releaseDateTime,
                releaseMillis = releaseDateTime.toInstant().toEpochMilli(),
                dataEntries = mutableSetOf(dataEntry),
                variants = variant?.let { mutableListOf(it) } ?: mutableListOf()
            )
            animeIndex[releaseDateTime] = record
            countryIndex.getOrPut(key.countryCode) { TreeMap() }
                .getOrPut(releaseDateTime) { mutableListOf() }
                .add(record)
        }
    }

    /**
     * Retrieves all grouped records based on optional filters.
     * Optimized using [countryIndex] and [TreeMap] operations ([subMap], [tailMap], [headMap]).
     *
     * @param minDateTime Start of the time range (inclusive).
     * @param maxDateTime End of the time range (inclusive).
     * @param countryCode Filter by country. If null, all countries are searched.
     * @param searchTypes Filter by language types (SUBTITLES, VOICE). If null, all types are included.
     * @return A sequence of [GroupedRecord] matching the criteria.
     */
    fun getAllRecords(
        minDateTime: ZonedDateTime? = null,
        maxDateTime: ZonedDateTime? = null,
        countryCode: CountryCode? = null,
        searchTypes: Array<LangType>? = null
    ): Sequence<GroupedRecord> {
        val trees = if (countryCode != null) {
            countryIndex[countryCode]?.let { listOf(it) } ?: emptyList()
        } else {
            countryIndex.values
        }

        return trees.asSequence().flatMap { countryTree ->
            // Efficiently get a portion of the map based on the time range
            val subMap = when {
                minDateTime != null && maxDateTime != null -> countryTree.subMap(minDateTime, true, maxDateTime, true)
                minDateTime != null -> countryTree.tailMap(minDateTime, true)
                maxDateTime != null -> countryTree.headMap(maxDateTime, true)
                else -> countryTree
            }
            subMap.values.asSequence().flatMap { it.asSequence() }
        }.filter { record ->
            // Post-filtering by language type if specified
            searchTypes == null || record.dataEntries.any { it.langType in searchTypes }
        }
    }

    /**
     * Retrieves filtered and sorted grouped records.
     */
    fun filterAndSortRecords(
        filter: ((GroupedRecord) -> Boolean)? = null,
        comparator: Comparator<GroupedRecord>? = null,
        minDateTime: ZonedDateTime? = null,
        maxDateTime: ZonedDateTime? = null,
        countryCode: CountryCode? = null,
        searchTypes: Array<LangType>? = null
    ): Sequence<GroupedRecord> {
        val records = getAllRecords(minDateTime, maxDateTime, countryCode, searchTypes)
        val filtered = if (filter != null) records.filter(filter) else records
        return if (comparator != null) filtered.sortedWith(comparator) else filtered
    }

    /**
     * Retrieves all individual variant records ([DataRecord]) based on optional filters.
     */
    fun getAllDataRecords(
        minDateTime: ZonedDateTime? = null,
        maxDateTime: ZonedDateTime? = null,
        countryCode: CountryCode? = null,
        searchTypes: Array<LangType>? = null
    ): Sequence<DataRecord> = getAllRecords(minDateTime, maxDateTime, countryCode, searchTypes).flatMap { record ->
        record.dataEntries.asSequence()
            .filter { searchTypes == null || it.langType in searchTypes }
            .map { DataRecord(it, record) }
    }

    /**
     * Retrieves filtered and sorted individual variant records.
     */
    fun filterAndSortDataRecords(
        filter: ((DataRecord) -> Boolean)? = null,
        comparator: Comparator<DataRecord>? = null,
        minDateTime: ZonedDateTime? = null,
        maxDateTime: ZonedDateTime? = null,
        countryCode: CountryCode? = null,
        searchTypes: Array<LangType>? = null
    ): Sequence<DataRecord> {
        val records = getAllDataRecords(minDateTime, maxDateTime, countryCode, searchTypes)
        val filtered = if (filter != null) records.filter(filter) else records
        return if (comparator != null) filtered.sortedWith(comparator) else filtered
    }

    /**
     * Provides a paginated view of grouped records.
     *
     * @param page 1-based page number.
     * @param limit Number of items per page.
     * @return [Pageable] containing the requested records and total count.
     */
    fun pageableRecords(
        page: Int,
        limit: Int,
        filter: ((GroupedRecord) -> Boolean)? = null,
        comparator: Comparator<GroupedRecord>? = null,
        minDateTime: ZonedDateTime? = null,
        maxDateTime: ZonedDateTime? = null,
        countryCode: CountryCode? = null,
        searchTypes: Array<LangType>? = null
    ): Pageable<GroupedRecord> {
        // Collect into list to get the total size and perform pagination
        val filteredAndSorted =
            filterAndSortRecords(filter, comparator, minDateTime, maxDateTime, countryCode, searchTypes).toList()
        val total = filteredAndSorted.size.toLong()
        val pagedData = filteredAndSorted.asSequence()
            .drop((page - 1).coerceAtLeast(0) * limit)
            .take(limit)
            .toSet()

        return Pageable(pagedData, page, limit, total)
    }

    /**
     * Groups a collection of [fr.shikkanime.entities.EpisodeVariant] using the same logic as the indexer.
     * This method does not modify the global index.
     */
    fun group(variants: Collection<fr.shikkanime.entities.EpisodeVariant>): List<GroupedRecord> {
        val tempIndex = HashMap<CompositeKey, TreeMap<ZonedDateTime, GroupedRecord>>()

        variants.forEach { variant ->
            val key = CompositeKey(
                variant.mapping!!.anime!!.countryCode!!,
                variant.mapping!!.anime!!.uuid!!,
                variant.mapping!!.anime!!.slug!!,
                variant.mapping!!.episodeType!!
            )
            val releaseDateTime = variant.releaseDateTime
            val audioLocale = variant.audioLocale!!
            val langType = LangType.fromAudioLocale(key.countryCode, audioLocale)
            val dataEntry = DataEntry(variant.uuid!!, variant.mapping!!.uuid!!, audioLocale, langType)

            val animeIndex = tempIndex.getOrPut(key) { TreeMap() }
            val matchingMin = releaseDateTime.minusHours(2)
            val matchingMax = releaseDateTime.plusHours(2)
            val existingEntry = animeIndex.subMap(matchingMin, true, matchingMax, true).firstEntry()

            if (existingEntry != null) {
                existingEntry.value.dataEntries.add(dataEntry)
                existingEntry.value.variants.add(variant)
            } else {
                val record = GroupedRecord(
                    key = key,
                    releaseDateTime = releaseDateTime,
                    releaseMillis = releaseDateTime.toInstant().toEpochMilli(),
                    dataEntries = mutableSetOf(dataEntry),
                    variants = mutableListOf(variant)
                )
                animeIndex[releaseDateTime] = record
            }
        }

        return tempIndex.values.flatMap { it.values }
    }
}
