package fr.shikkanime.jobs.platforms.impl

import fr.shikkanime.jobs.SmartHttpClient
import fr.shikkanime.jobs.perf.PerfBudget
import fr.shikkanime.jobs.platforms.PlatformAnime
import fr.shikkanime.jobs.platforms.PlatformEpisode
import io.ktor.client.*
import io.ktor.client.engine.mock.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.utils.io.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.*
import java.lang.management.ManagementFactory
import java.time.ZonedDateTime
import kotlin.system.measureNanoTime
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
class AnimationDigitalNetworkPlatformPerformanceTest {

    private val sampleDate: ZonedDateTime = ZonedDateTime.parse("2026-09-21T12:00:00Z")
    private val specialShowTypes = listOf(AdnVideoType.PV, AdnVideoType.BONUS)
    private val trailerIndicators = listOf("Bande-annonce", "Bande annonce", "Court-métrage", "Opening", "Making-of")

    private fun getUsedMemoryMb(): Long {
        val runtime = Runtime.getRuntime()
        return (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024)
    }

    private fun runGc() {
        System.gc()
        Thread.sleep(100)
        System.gc()
        Thread.sleep(100)
    }

    /**
     * Runs the mapping logic under a budget guard: prints the measures and then asserts
     * the run stayed within [PerfBudget]. Throughput is the primary signal; allocation
     * catches a regression that stays fast but bloats the heap (e.g. a lost memoization
     * building a new list per video).
     */
    private fun measureAndAssert(
        label: String,
        episodeCount: Int,
        expectedOutputSize: Int,
        block: () -> List<PlatformEpisode>?
    ): List<PlatformEpisode>? {
        var output: List<PlatformEpisode>? = null
        val measurement = PerfBudget.measure { output = block() }

        val throughput = "%.0f".format(measurement.throughputPerSecond(episodeCount))
        val megabytes = measurement.allocatedBytes / (1024 * 1024)
        val bytesPerEpisode = measurement.allocatedBytes.toDouble() / episodeCount

        println("  [$label] duration=" + "%.0f".format(measurement.durationMs) + " ms")
        println("  [$label] throughput=$throughput episodes/sec")
        println("  [$label] allocated=$megabytes MB (${"%.0f".format(bytesPerEpisode)} B/episode)")

        val throughputFloor = PerfBudget.throughputFloor(episodeCount)
        val allocationCeiling = PerfBudget.allocationCeiling(episodeCount)

        assertTrue(
            measurement.throughputPerSecond(episodeCount) >= throughputFloor,
            "$label: throughput $throughput eps/s is below the floor " +
                "${"%.0f".format(throughputFloor)} eps/s (regression or slow runner)"
        )
        assertTrue(
            measurement.allocatedBytes <= allocationCeiling,
            "$label: allocated $megabytes MB exceeds the ceiling " +
                "${allocationCeiling / (1024 * 1024)} MB (memory regression)"
        )
        assertEquals(
            expectedOutputSize,
            output?.size,
            "$label: the mapping produced an unexpected number of episodes"
        )

        return output
    }

    private fun getGcStats(): Pair<Long, Long> {
        var count = 0L
        var time = 0L
        for (gcBean in ManagementFactory.getGarbageCollectorMXBeans()) {
            val c = gcBean.collectionCount
            val t = gcBean.collectionTime
            if (c > 0) count += c
            if (t > 0) time += t
        }
        return count to time
    }

    private fun createMockClient(jsonContent: String): SmartHttpClient {
        val mockEngine = MockEngine { _ ->
            respond(
                content = jsonContent,
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            )
        }
        val httpClient = HttpClient(mockEngine) {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
        }
        return SmartHttpClient(client = httpClient)
    }

    /**
     * Same client, but the calendar body is produced lazily and in chunks instead of
     * being materialised as one [String].
     *
     * Buffering the payload costs three copies of the document in memory at once: the
     * StringBuilder's char[] (2 bytes/char), the String returned by toString() (1 byte/char
     * once compressed to Latin-1), and the String that bodyAsText() materialises. For a
     * 1M-video calendar that is well over a gigabyte before a single episode is mapped.
     * Streaming keeps only the current chunk resident.
     */
    private fun createStreamingMockClient(videoCount: Int): SmartHttpClient {
        val mockEngine = MockEngine { _ ->
            respond(
                content = streamCalendar(videoCount),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            )
        }
        val httpClient = HttpClient(mockEngine) {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
        }
        return SmartHttpClient(client = httpClient)
    }

    /**
     * Emits `{"videos":[…]}` for [videoCount] videos as a sequence of small chunks, so
     * the test never has to materialise the whole document itself.
     */
    private fun streamCalendar(videoCount: Int): ByteReadChannel {
        val channel = ByteChannel()
        val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

        scope.launch {
            try {
                channel.writeString("{\"videos\":[")
                for (i in 1..videoCount) {
                    if (i > 1) channel.writeString(",")
                    channel.writeString(
                        """{"id":$i,"name":"Episode $i","shortNumber":"$i","type":"EPS",""" +
                            """"image2x":"https://image.animationdigitalnetwork.com/video/$i/100x100/eps",""" +
                            """"summary":"Summary $i","releaseDate":"2026-09-21T12:00:00Z",""" +
                            """"show":{"id":1,"title":"One Piece","summary":"Show summary\\nDescription",""" +
                            """"image2x":"https://image.animationdigitalnetwork.com/show/1/100x100/portrait-with-logo",""" +
                            """"genres":["Animation japonaise"]}}"""
                    )
                }
                channel.writeString("]}")
                channel.close()
            } catch (e: Throwable) {
                channel.close(e)
            } finally {
                scope.cancel()
            }
        }

        return channel
    }

    @Test
    @Order(1)
    @DisplayName("Benchmark 1: 1M episodes - SAME anime (Processing loop)")
    fun `benchmark 1M episodes - SAME anime`() {
        println("\n=======================================================")
        println("TEST 1: 1,000,000 EPISODES - SAME ANIME (1 show, 1M episodes)")
        println("=======================================================")

        runGc()
        val memBeforeGen = getUsedMemoryMb()

        val show = AdnShow(
            id = 1,
            title = "One Piece",
            summary = "Show description\nWith multiple lines",
            image2x = "https://image.animationdigitalnetwork.fr/show/1/100x100/portrait-with-logo",
            genres = listOf("Animation japonaise", "Action")
        )

        // Generate 1M videos sharing the same show instance
        val videos = ArrayList<AdnVideo>(1_000_000)
        for (i in 1..1_000_000) {
            videos.add(
                AdnVideo(
                    id = i,
                    name = "Episode $i",
                    shortNumber = "$i",
                    type = AdnVideoType.EPS,
                    image2x = "https://image.animationdigitalnetwork.fr/video/$i/100x100/eps",
                    summary = "Summary of episode $i",
                    releaseDate = sampleDate,
                    show = show
                )
            )
        }

        val memAfterGen = getUsedMemoryMb()
        println("Memory used for 1M AdnVideo objects: ${memAfterGen - memBeforeGen} MB")

        val (gcCountBefore, gcTimeBefore) = getGcStats()

        // Run the EXACT logic from AnimationDigitalNetworkPlatform.fetchLatestEpisodes
        measureAndAssert(label = "benchmark-1-same-anime", episodeCount = 1_000_000, expectedOutputSize = 1_000_000) {
            val animes = mutableMapOf<Int, PlatformAnime>()
            val invalidShowIds = mutableSetOf<Int>()

            videos.mapNotNull { video ->
                if (video.type in specialShowTypes) return@mapNotNull null
                if (trailerIndicators.any { video.shortNumber.startsWith(it) }) return@mapNotNull null

                val currentShow = video.show
                if (currentShow.id in invalidShowIds) return@mapNotNull null

                val anime = animes[currentShow.id] ?: run {
                    if (currentShow.genres.none { it.contains("Animation ") }) {
                        invalidShowIds.add(currentShow.id)
                        return@mapNotNull null
                    }

                    PlatformAnime(
                        id = currentShow.id.toString(),
                        title = currentShow.title,
                        description = currentShow.summary?.replace("\n", ""),
                        thumbnail = currentShow.thumbnail,
                    ).also { animes[currentShow.id] = it }
                }

                PlatformEpisode(
                    anime = anime,
                    id = video.id.toString(),
                    title = video.name,
                    description = video.summary,
                    image = video.image,
                    releaseDateTime = video.releaseDate
                )
            }
        }

        val (gcCountAfter, gcTimeAfter) = getGcStats()
        val memAfterProcess = getUsedMemoryMb()

        println("Memory delta during processing: ${memAfterProcess - memAfterGen} MB (Total: $memAfterProcess MB)")
        println("GC Collections during test: ${gcCountAfter - gcCountBefore}, GC Time: ${gcTimeAfter - gcTimeBefore} ms")
    }

    @Test
    @Order(2)
    @DisplayName("Benchmark 2: 1M episodes - DIFFERENT anime (1M distinct shows)")
    fun `benchmark 1M episodes - DIFFERENT anime`() {
        println("\n=======================================================")
        println("TEST 2: 1,000,000 EPISODES - DIFFERENT ANIME (1M distinct shows)")
        println("=======================================================")

        runGc()
        val memBeforeGen = getUsedMemoryMb()

        val videos = ArrayList<AdnVideo>(1_000_000)
        for (i in 1..1_000_000) {
            val show = AdnShow(
                id = i,
                title = "Anime Title $i",
                summary = "Anime description $i\nLine 2",
                image2x = "https://image.animationdigitalnetwork.fr/show/$i/100x100/portrait-with-logo",
                genres = listOf("Animation japonaise", "Action")
            )
            videos.add(
                AdnVideo(
                    id = i,
                    name = "Episode 1",
                    shortNumber = "1",
                    type = AdnVideoType.EPS,
                    image2x = "https://image.animationdigitalnetwork.fr/video/$i/100x100/eps",
                    summary = "Summary of episode $i",
                    releaseDate = sampleDate,
                    show = show
                )
            )
        }

        val memAfterGen = getUsedMemoryMb()
        println("Memory used for 1M AdnVideo + 1M AdnShow objects: ${memAfterGen - memBeforeGen} MB")

        val (gcCountBefore, gcTimeBefore) = getGcStats()

        measureAndAssert(label = "benchmark-2-distinct-anime", episodeCount = 1_000_000, expectedOutputSize = 1_000_000) {
            val animes = mutableMapOf<Int, PlatformAnime>()
            val invalidShowIds = mutableSetOf<Int>()

            videos.mapNotNull { video ->
                if (video.type in specialShowTypes) return@mapNotNull null
                if (trailerIndicators.any { video.shortNumber.startsWith(it) }) return@mapNotNull null

                val currentShow = video.show
                if (currentShow.id in invalidShowIds) return@mapNotNull null

                val anime = animes[currentShow.id] ?: run {
                    if (currentShow.genres.none { it.contains("Animation ") }) {
                        invalidShowIds.add(currentShow.id)
                        return@mapNotNull null
                    }

                    PlatformAnime(
                        id = currentShow.id.toString(),
                        title = currentShow.title,
                        description = currentShow.summary?.replace("\n", ""),
                        thumbnail = currentShow.thumbnail,
                    ).also { animes[currentShow.id] = it }
                }

                PlatformEpisode(
                    anime = anime,
                    id = video.id.toString(),
                    title = video.name,
                    description = video.summary,
                    image = video.image,
                    releaseDateTime = video.releaseDate
                )
            }
        }

        val (gcCountAfter, gcTimeAfter) = getGcStats()
        val memAfterProcess = getUsedMemoryMb()

        println("Memory delta during processing: ${memAfterProcess - memAfterGen} MB (Total: $memAfterProcess MB)")
        println("GC Collections during test: ${gcCountAfter - gcCountBefore}, GC Time: ${gcTimeAfter - gcTimeBefore} ms")
    }

    @Test
    @Order(3)
    @DisplayName("Benchmark 3: 1M episodes - REALISTIC CATALOG with filters (PV, Trailers, Non-Animation)")
    fun `benchmark 1M episodes - REALISTIC catalog`() {
        println("\n=======================================================")
        println("TEST 3: 1,000,000 EPISODES - 1,000 ANIME x 1,000 EPISODES (with PV & trailers)")
        println("=======================================================")

        runGc()
        val memBeforeGen = getUsedMemoryMb()

        val shows = Array(1000) { i ->
            val isLiveAction = (i % 10 == 0) // 10% non-animation shows
            AdnShow(
                id = i + 1,
                title = "Anime Title ${i + 1}",
                summary = "Anime description ${i + 1}\nLine 2",
                image2x = "https://image.animationdigitalnetwork.fr/show/${i + 1}/100x100/portrait-with-logo",
                genres = if (isLiveAction) listOf("Live Action", "Drama") else listOf("Animation japonaise", "Action")
            )
        }

        val videos = ArrayList<AdnVideo>(1_000_000)
        for (i in 1..1_000_000) {
            val show = shows[i % 1000]
            val videoType = when {
                i % 50 == 0 -> AdnVideoType.PV // 2% PV
                i % 100 == 0 -> AdnVideoType.BONUS // 1% Bonus
                else -> AdnVideoType.EPS
            }
            val shortNumber = when {
                i % 40 == 0 -> "Bande-annonce 1"
                i % 80 == 0 -> "Opening 1"
                else -> "${i % 1000 + 1}"
            }

            videos.add(
                AdnVideo(
                    id = i,
                    name = "Episode $i",
                    shortNumber = shortNumber,
                    type = videoType,
                    image2x = "https://image.animationdigitalnetwork.fr/video/$i/100x100/eps",
                    summary = "Summary of episode $i",
                    releaseDate = sampleDate,
                    show = show
                )
            )
        }

        val memAfterGen = getUsedMemoryMb()
        println("Memory used for 1M AdnVideo + 1,000 AdnShow: ${memAfterGen - memBeforeGen} MB")

        val (gcCountBefore, gcTimeBefore) = getGcStats()

        val resultList = measureAndAssert(label = "benchmark-3-realistic-catalog", episodeCount = 1_000_000, expectedOutputSize = 900_000) {
            val animes = mutableMapOf<Int, PlatformAnime>()
            val invalidShowIds = mutableSetOf<Int>()

            videos.mapNotNull { video ->
                if (video.type in specialShowTypes) return@mapNotNull null
                if (trailerIndicators.any { video.shortNumber.startsWith(it) }) return@mapNotNull null

                val currentShow = video.show
                if (currentShow.id in invalidShowIds) return@mapNotNull null

                val anime = animes[currentShow.id] ?: run {
                    if (currentShow.genres.none { it.contains("Animation ") }) {
                        invalidShowIds.add(currentShow.id)
                        return@mapNotNull null
                    }

                    PlatformAnime(
                        id = currentShow.id.toString(),
                        title = currentShow.title,
                        description = currentShow.summary?.replace("\n", ""),
                        thumbnail = currentShow.thumbnail,
                    ).also { animes[currentShow.id] = it }
                }

                PlatformEpisode(
                    anime = anime,
                    id = video.id.toString(),
                    title = video.name,
                    description = video.summary,
                    image = video.image,
                    releaseDateTime = video.releaseDate
                )
            }
        }

        val (gcCountAfter, gcTimeAfter) = getGcStats()
        val memAfterProcess = getUsedMemoryMb()

        println("Output episodes count: ${resultList?.size} (filtered out ~${1_000_000 - (resultList?.size ?: 0)})")
        println("Memory delta during processing: ${memAfterProcess - memAfterGen} MB (Total: $memAfterProcess MB)")
        println("GC Collections during test: ${gcCountAfter - gcCountBefore}, GC Time: ${gcTimeAfter - gcTimeBefore} ms")
    }

    @Test
    @Order(4)
    @DisplayName("Benchmark 4: Bottleneck Analysis - Regex recompilation impact (with landscape-with-logo)")
    fun `benchmark regex bottleneck analysis`() {
        println("\n=======================================================")
        println("TEST 4: BOTTLENECK DEEP-DIVE - Regex vs Precompiled Regex")
        println("=======================================================")

        val sampleUrl = "https://image.animationdigitalnetwork.fr/video/12345/100x100/eps"
        val count = 1_000_000

        // 1. Recompiling 3 regexes every time
        val currentDurationMs = measureNanoTime {
            var dummy: String? = null
            for (i in 1..count) {
                dummy = sampleUrl.replace("\\d+x\\d+".toRegex(), "1920x1080")
                    .replace("/eps$".toRegex(), "/eps.width=1920,height=1080,quality=100")
                    .replace("/landscape-with-logo$".toRegex(), "/landscape-with-logo.width=1920,height=1080,quality=100")
            }
        } / 1_000_000.0

        println("Current approach (3 .toRegex() recompiled per episode * 1M = 3M compilations): ${"%.2f".format(currentDurationMs)} ms")

        // 2. Precompiled regex
        val regex1 = "\\d+x\\d+".toRegex()
        val regex2 = "/eps$".toRegex()
        val regex3 = "/landscape-with-logo$".toRegex()
        val precompiledDurationMs = measureNanoTime {
            var dummy: String? = null
            for (i in 1..count) {
                dummy = sampleUrl.replace(regex1, "1920x1080")
                    .replace(regex2, "/eps.width=1920,height=1080,quality=100")
                    .replace(regex3, "/landscape-with-logo.width=1920,height=1080,quality=100")
            }
        } / 1_000_000.0

        println("Precompiled regex (reusing 3 Regex instances for 1M iterations): ${"%.2f".format(precompiledDurationMs)} ms")

        val speedup = currentDurationMs / precompiledDurationMs
        println("Speedup factor for image URL transformation: ${"%.2f".format(speedup)}x faster!")

        // The point of the precompiled regexes is that they are meaningfully faster. If a
        // future JIT change makes recompiling free, this assertion is the signal to drop
        // the optimisation rather than keep dead complexity.
        assertTrue(
            speedup >= PerfBudget.MIN_REGEX_SPEEDUP,
            "benchmark-4: precompiled regex is only ${"%.2f".format(speedup)}x faster than recompiling " +
                "(expected at least ${PerfBudget.MIN_REGEX_SPEEDUP}x) — the optimisation no longer pays off"
        )
    }

    @Test
    @Order(5)
    @DisplayName("Benchmark 5: Bottleneck Analysis - MutableMap Pre-sizing vs Default")
    fun `benchmark map presizing analysis`() {
        println("\n=======================================================")
        println("TEST 5: BOTTLENECK DEEP-DIVE - Map Resizing & Boxing (1M insertions)")
        println("=======================================================")

        val dummyAnime = PlatformAnime("1", "Title", "Desc", "Thumb")

        // Default HashMap (capacity 16, rehashes 16+ times)
        val defaultMapDurationMs = measureNanoTime {
            val map = mutableMapOf<Int, PlatformAnime>()
            for (i in 1..1_000_000) {
                map[i] = dummyAnime
            }
        } / 1_000_000.0
        println("Default mutableMapOf() (multiple rehashes): ${"%.2f".format(defaultMapDurationMs)} ms")

        // Pre-sized HashMap
        val preSizedMapDurationMs = measureNanoTime {
            val map = HashMap<Int, PlatformAnime>((1_000_000 / 0.75f).toInt() + 1)
            for (i in 1..1_000_000) {
                map[i] = dummyAnime
            }
        } / 1_000_000.0
        println("Pre-sized HashMap(1_333_334) (0 rehashes): ${"%.2f".format(preSizedMapDurationMs)} ms")

        val speedup = defaultMapDurationMs / preSizedMapDurationMs
        println("Speedup from pre-sizing: ${"%.2f".format(speedup)}x faster!")

        assertTrue(
            speedup >= PerfBudget.MIN_MAP_PRESIZE_SPEEDUP,
            "benchmark-5: pre-sizing the map is only ${"%.2f".format(speedup)}x faster than the default " +
                "(expected at least ${PerfBudget.MIN_MAP_PRESIZE_SPEEDUP}x) — the optimisation no longer pays off"
        )
    }

    @Test
    @Order(6)
    @DisplayName("Benchmark 6: End-to-end through SmartHttpClient with a streamed calendar")
    fun `benchmark end to end with MockEngine`() = runBlocking {
        println("\n=======================================================")
        println("TEST 6: FULL END-TO-END with SmartHttpClient + MockEngine (streamed body)")
        println("=======================================================")

        runGc()

        // The calendar is streamed rather than buffered as one String: buffering a payload
        // this size costs three copies in memory at once (the StringBuilder's char[] at 2
        // bytes/char, the String from toString(), and the String bodyAsText() materialises).
        //
        // Note that streaming only removes the copies the TEST creates. SmartHttpClient
        // itself still calls bodyAsText() to compute the content hash before response.body<T>(),
        // so the client buffers the document regardless. The real ADN calendar holds ~33
        // videos, so this is not a production concern — but it is the reason a 1M-video
        // payload cannot be benchmarked end-to-end on a small heap.
        val videoCount = 100_000
        println("Streaming a calendar of $videoCount videos...")

        val client = createStreamingMockClient(videoCount)
        val platform = AnimationDigitalNetworkPlatform(client)

        // 1st call: Full pipeline
        val (gcCount1, gcTime1) = getGcStats()
        var episodes1: List<PlatformEpisode>? = null
        val duration1 = measureNanoTime {
            episodes1 = platform.fetchLatestEpisodes()
        } / 1_000_000.0
        val (gcCount2, gcTime2) = getGcStats()

        println("\n--- First call (hasChanged = true: Network + bodyAsText + hash + JSON parse + Mapping) ---")
        println("Total Duration: ${"%.2f".format(duration1)} ms (${"%.2f".format(duration1 / 1000.0)} s)")
        println("Fetched: ${episodes1?.size} episodes")
        println("GC Collections: ${gcCount2 - gcCount1}, GC Time: ${gcTime2 - gcTime1} ms")

        // 2nd call: Cache hit (hasChanged = false)
        val duration2 = measureNanoTime {
            val episodes2 = platform.fetchLatestEpisodes()
            assertEquals(episodes1?.size, episodes2.size, "benchmark-6: the cached call returned a different episode count")
        } / 1_000_000.0

        val cacheSpeedup = duration1 / duration2
        println("\n--- Second call (hasChanged = false: Cache hit / TTL fresh) ---")
        println("Duration: ${"%.4f".format(duration2)} ms")
        println("Speedup from cache: ${"%.1f".format(cacheSpeedup)}x faster")

        // The whole point of the memoized list is that the second call is a cache hit. If
        // this collapses, the hasChanged gate or the TTL logic has been broken.
        assertTrue(
            cacheSpeedup >= PerfBudget.MIN_CACHE_SPEEDUP,
            "benchmark-6: the cached call is only ${"%.1f".format(cacheSpeedup)}x faster than the first " +
                "(expected at least ${PerfBudget.MIN_CACHE_SPEEDUP}x) — caching no longer works"
        )
    }
}
