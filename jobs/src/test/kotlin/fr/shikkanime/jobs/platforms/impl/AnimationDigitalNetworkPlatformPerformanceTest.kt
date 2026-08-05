package fr.shikkanime.jobs.platforms.impl

import fr.shikkanime.jobs.SmartHttpClient
import fr.shikkanime.jobs.platforms.PlatformAnime
import fr.shikkanime.jobs.platforms.PlatformEpisode
import io.ktor.client.*
import io.ktor.client.engine.mock.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.*
import java.lang.management.ManagementFactory
import java.time.ZonedDateTime
import kotlin.system.measureNanoTime

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
        var resultList: List<PlatformEpisode>? = null
        val durationNanos = measureNanoTime {
            val animes = mutableMapOf<Int, PlatformAnime>()
            val invalidShowIds = mutableSetOf<Int>()

            resultList = videos.mapNotNull { video ->
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

        val durationMs = durationNanos / 1_000_000.0
        println("Duration: ${"%.2f".format(durationMs)} ms (${"%.2f".format(durationMs / 1000.0)} s)")
        println("Throughput: ${"%.0f".format(1_000_000.0 / (durationMs / 1000.0))} episodes/sec")
        println("Output episodes count: ${resultList?.size}")
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

        var resultList: List<PlatformEpisode>? = null
        val durationNanos = measureNanoTime {
            val animes = mutableMapOf<Int, PlatformAnime>()
            val invalidShowIds = mutableSetOf<Int>()

            resultList = videos.mapNotNull { video ->
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

        val durationMs = durationNanos / 1_000_000.0
        println("Duration: ${"%.2f".format(durationMs)} ms (${"%.2f".format(durationMs / 1000.0)} s)")
        println("Throughput: ${"%.0f".format(1_000_000.0 / (durationMs / 1000.0))} episodes/sec")
        println("Output episodes count: ${resultList?.size}")
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

        var resultList: List<PlatformEpisode>? = null
        val durationNanos = measureNanoTime {
            val animes = mutableMapOf<Int, PlatformAnime>()
            val invalidShowIds = mutableSetOf<Int>()

            resultList = videos.mapNotNull { video ->
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

        val durationMs = durationNanos / 1_000_000.0
        println("Duration: ${"%.2f".format(durationMs)} ms (${"%.2f".format(durationMs / 1000.0)} s)")
        println("Throughput: ${"%.0f".format(1_000_000.0 / (durationMs / 1000.0))} episodes/sec")
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
        println("Speedup factor for image URL transformation: ${"%.2f".format(currentDurationMs / precompiledDurationMs)}x faster!")
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
        println("Speedup from pre-sizing: ${"%.2f".format(defaultMapDurationMs / preSizedMapDurationMs)}x faster!")
    }

    @Test
    @Order(6)
    @DisplayName("Benchmark 6: End-to-End via AnimationDigitalNetworkPlatform.fetchLatestEpisodes with MockEngine (1M episodes)")
    fun `benchmark end to end with MockEngine 1M`() = runBlocking {
        println("\n=======================================================")
        println("TEST 6: FULL END-TO-END with SmartHttpClient + MockEngine (1,000,000 EPISODES)")
        println("=======================================================")

        runGc()

        println("Generating JSON for 1,000,000 episodes (same anime)...")
        val sb = StringBuilder(220 * 1024 * 1024)
        sb.append("{\"videos\":[")
        for (i in 1..1_000_000) {
            if (i > 1) sb.append(",")
            sb.append("{\"id\":").append(i)
                .append(",\"name\":\"Episode ").append(i).append("\"")
                .append(",\"shortNumber\":\"").append(i).append("\"")
                .append(",\"type\":\"EPS\"")
                .append(",\"image2x\":\"https://image.animationdigitalnetwork.fr/video/").append(i).append("/100x100/eps\"")
                .append(",\"summary\":\"Summary ").append(i).append("\"")
                .append(",\"releaseDate\":\"2026-09-21T12:00:00Z\"")
                .append(",\"show\":{\"id\":1")
                .append(",\"title\":\"One Piece\"")
                .append(",\"summary\":\"Show summary\\nDescription\"")
                .append(",\"image2x\":\"https://image.animationdigitalnetwork.fr/show/1/100x100/portrait-with-logo\"")
                .append(",\"genres\":[\"Animation japonaise\"]}}")
        }
        sb.append("]}")
        val json = sb.toString()
        val jsonMb = json.length / (1024 * 1024)
        println("JSON generated: $jsonMb MB")

        val client = createMockClient(json)
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
            assert(episodes2.size == episodes1?.size)
        } / 1_000_000.0

        println("\n--- Second call (hasChanged = false: Cache hit / TTL fresh) ---")
        println("Duration: ${"%.4f".format(duration2)} ms")
        println("Speedup from cache: ${"%.1f".format(duration1 / duration2)}x faster")
    }
}
