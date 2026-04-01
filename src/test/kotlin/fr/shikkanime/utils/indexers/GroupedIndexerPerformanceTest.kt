package fr.shikkanime.utils.indexers

import fr.shikkanime.entities.enums.CountryCode
import fr.shikkanime.entities.enums.EpisodeType
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.ZonedDateTime
import java.util.*
import kotlin.system.measureTimeMillis

class GroupedIndexerPerformanceTest {

    @BeforeEach
    fun setUp() {
        GroupedIndexer.clear()
    }

    @Test
    fun `performance test with millions of entries`() {
        val entryCount = 1_000_000
        val animeCount = 5_000
        val countryCodes = CountryCode.entries
        val episodeTypes = EpisodeType.entries

        val random = Random(42)

        val animeKeys = (1..animeCount).map {
            GroupedIndexer.CompositeKey(
                countryCode = countryCodes[random.nextInt(countryCodes.size)],
                animeUuid = UUID.randomUUID(),
                animeSlug = "anime-$it",
                episodeType = episodeTypes[random.nextInt(episodeTypes.size)]
            )
        }

        println("Generating $entryCount entries...")
        val timeToAdd = measureTimeMillis {
            for (i in 1..entryCount) {
                val key = animeKeys[random.nextInt(animeKeys.size)]
                // Simulate dates over 2 years for broad distribution
                val releaseDateTime = ZonedDateTime.now()
                    .minusDays(random.nextInt(730).toLong())
                    .minusMinutes(random.nextInt(1440).toLong())

                GroupedIndexer.add(
                    key = key,
                    variantUuid = UUID.randomUUID(),
                    mappingUuid = UUID.randomUUID(),
                    releaseDateTime = releaseDateTime,
                    audioLocale = if (random.nextBoolean()) "ja-JP" else "fr-FR"
                )
            }
        }
        println("Time to add $entryCount entries: $timeToAdd ms")

        // Calendar Test (similar to AnimeService.getCalendar)
        val startAtPreviousWeek = ZonedDateTime.now().minusWeeks(1)
        val endOfCurrentWeek = ZonedDateTime.now().plusWeeks(1)
        val targetCountryCode = CountryCode.FR

        println("Calendar performance test (date range filtering)...")
        repeat(5) { i ->
            val timeCalendar = measureTimeMillis {
                val results = GroupedIndexer.filterAndSortDataRecords(
                    filter = { (_, record) ->
                        (record.releaseDateTime.isAfter(startAtPreviousWeek) && record.releaseDateTime.isBefore(
                            endOfCurrentWeek
                        ))
                    },
                    comparator = compareBy(
                        { it.record.releaseMillis },
                        { it.record.key.animeSlug },
                        { it.record.key.episodeType }),
                    countryCode = targetCountryCode
                ).toList()
                if (i == 0) println("Calendar results: ${results.size}")
            }
            println("Run ${i + 1} Calendar (current): $timeCalendar ms")
        }

        // Simulate optimization with subMap
        println("Calendar performance test (optimized with subMap)...")
        repeat(5) { i ->
            val timeCalendarOptimized = measureTimeMillis {
                val results = GroupedIndexer.filterAndSortDataRecords(
                    comparator = compareBy(
                        { it.record.releaseMillis },
                        { it.record.key.animeSlug },
                        { it.record.key.episodeType }),
                    minDateTime = startAtPreviousWeek,
                    maxDateTime = endOfCurrentWeek,
                    countryCode = targetCountryCode
                ).toList()
                if (i == 0) println("Optimized calendar results: ${results.size}")
            }
            println("Run ${i + 1} Calendar (optimized): $timeCalendarOptimized ms")
        }

        // API Test (similar to GroupedEpisodeRepository.findAll)
        println("API performance test (pagination and global sort)...")
        repeat(5) { i ->
            val timeApi = measureTimeMillis {
                val pageable = GroupedIndexer.pageableRecords(
                    page = 1,
                    limit = 30,
                    countryCode = targetCountryCode,
                    comparator = compareByDescending<GroupedIndexer.GroupedRecord> { it.releaseMillis }
                        .thenBy { it.key.animeSlug }
                )
                if (i == 0) println("API results (total): ${pageable.total}")
            }
            println("Run ${i + 1} API: $timeApi ms")
        }

        // Specific test: Accessing page 100
        println("API performance test (page 100)...")
        val timeApiPage100 = measureTimeMillis {
            GroupedIndexer.pageableRecords(
                page = 100,
                limit = 30,
                countryCode = targetCountryCode,
                comparator = compareByDescending<GroupedIndexer.GroupedRecord> { it.releaseMillis }
                    .thenBy { it.key.animeSlug }
            )
        }
        println("API Page 100 time: $timeApiPage100 ms")
    }
}
