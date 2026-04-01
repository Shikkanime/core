package fr.shikkanime.utils.indexers

import fr.shikkanime.entities.enums.CountryCode
import fr.shikkanime.entities.enums.EpisodeType
import fr.shikkanime.entities.enums.LangType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.ZonedDateTime
import java.util.*

class GroupedIndexerFilteringTest {

    @BeforeEach
    fun setUp() {
        GroupedIndexer.clear()
    }

    @Test
    fun `filter by lang type should work`() {
        val animeUuid = UUID.randomUUID()
        val key = GroupedIndexer.CompositeKey(CountryCode.FR, animeUuid, "anime-1", EpisodeType.EPISODE)
        val now = ZonedDateTime.now()

        // Add an episode with SUBTITLES (ja-JP)
        GroupedIndexer.add(key, UUID.randomUUID(), UUID.randomUUID(), now, "ja-JP")
        // Add an episode with VOICE (fr-FR) in the same 2-hour window
        GroupedIndexer.add(key, UUID.randomUUID(), UUID.randomUUID(), now.plusMinutes(5), "fr-FR")

        // Add another episode 3 hours later, SUBTITLES only
        val later = now.plusHours(3)
        GroupedIndexer.add(key, UUID.randomUUID(), UUID.randomUUID(), later, "ja-JP")

        // 1. All records
        val allRecords = GroupedIndexer.getAllRecords().toList()
        assertEquals(2, allRecords.size)

        // 2. Filter by SUBTITLES (VOSTFR)
        val subtitleRecords = GroupedIndexer.getAllRecords(searchTypes = arrayOf(LangType.SUBTITLES)).toList()
        assertEquals(2, subtitleRecords.size) // Both records have at least one SUBTITLES entry

        // 3. Filter by VOICE (VF)
        val voiceRecords = GroupedIndexer.getAllRecords(searchTypes = arrayOf(LangType.VOICE)).toList()
        assertEquals(1, voiceRecords.size) // Only the first record has a VOICE entry

        // 4. Filter DataRecords by VOICE
        val voiceDataRecords = GroupedIndexer.getAllDataRecords(searchTypes = arrayOf(LangType.VOICE)).toList()
        assertEquals(1, voiceDataRecords.size)
        assertEquals(LangType.VOICE, voiceDataRecords[0].data.langType)

        // 5. Filter DataRecords by SUBTITLES
        val subtitleDataRecords = GroupedIndexer.getAllDataRecords(searchTypes = arrayOf(LangType.SUBTITLES)).toList()
        assertEquals(2, subtitleDataRecords.size)
        subtitleDataRecords.forEach { assertEquals(LangType.SUBTITLES, it.data.langType) }
    }
}
