package fr.shikkanime.utils.indexers

import fr.shikkanime.entities.enums.CountryCode
import fr.shikkanime.entities.enums.EpisodeType
import fr.shikkanime.entities.enums.LangType
import java.time.ZonedDateTime
import java.util.BitSet
import java.util.EnumMap
import java.util.TreeMap
import java.util.UUID

object GroupedEpisodeBitmapIndexer {
    private const val RANGE_HOURS: Long = 2

    data class GroupRecord(
        val countryCode: CountryCode,
        val animeUuid: UUID,
        val animeSlug: String,
        val episodeType: EpisodeType,
        val releaseDateTime: ZonedDateTime,
        val variantUuids: MutableSet<UUID> = mutableSetOf(),
    )

    val records = mutableListOf<GroupRecord>()
    private val countryBitmaps = EnumMap<CountryCode, BitSet>(CountryCode::class.java)
    private val animeUuidBitmaps = HashMap<UUID, BitSet>()
    private val animeSlugBitmaps = HashMap<String, BitSet>()
    private val episodeTypeBitmaps = EnumMap<EpisodeType, BitSet>(EpisodeType::class.java)
    private val releaseDateTreeMap = TreeMap<ZonedDateTime, BitSet>()
    private val langTypeBitmaps = EnumMap<LangType, BitSet>(LangType::class.java)

    fun query(
        countryCode: CountryCode? = null,
        animeUuid: UUID? = null,
        animeSlug: String? = null,
        episodeType: EpisodeType? = null,
        minReleaseDateTime: ZonedDateTime? = null,
        maxReleaseDateTime: ZonedDateTime? = null,
        langTypes: Array<LangType>? = null
    ): List<GroupRecord> {
        val result = BitSet(records.size)
        result.set(0, records.size)

        countryCode?.let { result.and(countryBitmaps[it] ?: BitSet()) }
        animeUuid?.let { result.and(animeUuidBitmaps[it] ?: BitSet()) }
        animeSlug?.let { result.and(animeSlugBitmaps[it] ?: BitSet()) }
        episodeType?.let { result.and(episodeTypeBitmaps[it] ?: BitSet()) }

        if (minReleaseDateTime != null && maxReleaseDateTime != null) {
            val dateTimeResults = releaseDateTreeMap.subMap(minReleaseDateTime, true, maxReleaseDateTime, true)
            val dateBits = BitSet(records.size)
            dateTimeResults.values.forEach { dateBits.or(it) }
            result.and(dateBits)
        }

        langTypes?.let { langTypes ->
            val langTypeBits = BitSet(records.size)
            langTypes.forEach { langTypeBits.or(langTypeBitmaps[it] ?: BitSet()) }
            result.and(langTypeBits)
        }

        return result.stream().mapToObj { records[it] }.toList()
    }

    fun add(
        countryCode: CountryCode,
        animeUuid: UUID,
        animeSlug: String,
        episodeType: EpisodeType,
        variantUuid: UUID,
        releaseDateTime: ZonedDateTime,
        audioLocale: String
    ) {
        val minReleaseDateTime = releaseDateTime.minusHours(RANGE_HOURS)
        val maxReleaseDateTime = releaseDateTime.plusHours(RANGE_HOURS)

        val record = query(
            countryCode = countryCode,
            animeUuid = animeUuid,
            animeSlug = animeSlug,
            episodeType = episodeType,
            minReleaseDateTime = minReleaseDateTime,
            maxReleaseDateTime = maxReleaseDateTime
        ).singleOrNull()

        if (record != null) {
            record.variantUuids.add(variantUuid)
        } else {
            val newRecord = GroupRecord(
                countryCode = countryCode,
                animeUuid = animeUuid,
                animeSlug = animeSlug,
                episodeType = episodeType,
                releaseDateTime = releaseDateTime,
                variantUuids = mutableSetOf(variantUuid)
            )

            val index = records.size
            records.add(newRecord)
            countryBitmaps.getOrPut(countryCode) { BitSet() }.set(index)
            animeUuidBitmaps.getOrPut(animeUuid) { BitSet() }.set(index)
            animeSlugBitmaps.getOrPut(animeSlug) { BitSet() }.set(index)
            episodeTypeBitmaps.getOrPut(episodeType) { BitSet() }.set(index)
            releaseDateTreeMap.getOrPut(releaseDateTime) { BitSet() }.set(index)
            langTypeBitmaps.getOrPut(LangType.fromAudioLocale(countryCode, audioLocale)) { BitSet() }.set(index)
        }
    }
}