# Performance Guide

This guide establishes the performance principles and testing standards for Shikkanime.
All AI agents and developers must adhere to these directives when implementing or optimizing features.

---

## 1. Core Runtime Principles

- **Avoid redundant external calls**: Always check and leverage fresh cached data before querying remote APIs.
- **Deduplicate input eagerly**: Group and normalize raw items before initiating expensive downstream processing.
- **Precompile static patterns**: Never dynamically compile regular expressions inside hot loops or mapping methods.
  Extract static patterns to top-level `Regex` constants.
- **Minimize heap allocations in hot paths**: Favor primitive types, direct string manipulation (e.g. `removeSuffix`),
  and reuse cached domain objects instead of allocating intermediate collections.
- **Limit database round trips**: Use batch queries, projections, and bulk inserts instead of N+1 select loops.
- **Keep retry mechanisms bounded**: Exponential backoff with sensible ceilings to prevent thundering herd issues.

---

## 2. Performance Testing Architecture (`:jobs:perfTest`)

Performance benchmarks are located in `jobs/src/perfTest/kotlin/` and run separately from standard unit tests
via `./gradlew :jobs:perfTest`. They operate under a dedicated 4GB heap and are compiled with Gradle compilation
association to access internal platform methods.

### Key Framework Components

- **`BasePlatformPerformanceTest`**: Abstract base class providing `measureAndAssert`. Handles garbage
  collection isolation, profiler invocation, diagnostic report formatting, and budget assertions.
- **`PerfProfiler`**: Uses JVM `ThreadMXBean` to measure wall-clock duration, thread CPU time, thread user
  CPU time, thread-allocated bytes, and `GarbageCollectorMXBean` for pause times and collection counts.
- **`PerfBudget`**: Defines machine-resilient baseline numbers (`REFERENCE_EPISODES_PER_SECOND`,
  `REFERENCE_ALLOCATION_BYTES_PER_EPISODE`) scaled by `BUDGET_MULTIPLIER` (3.0x).
- **`PerfReporter`**: Emits ASCII diagnostic tables and pinpoints execution bottlenecks (`CPU-BOUND`,
  `GC PRESSURE`, `HIGH MEMORY CHURN`, `BALANCED`).

---

## 3. Mandatory Rules for AI Agents Writing Performance Tests

When implementing or evolving performance tests for streaming platforms (e.g. ADN, Crunchyroll, Netflix):

### Rule 1: Always Test Real Production Code
**Never** copy-paste or duplicate mapping, parsing, or filtering loops inside test methods.
- Expose internal platform conversion methods (e.g. `platform.convertVideos(...)`).
- If a developer modifies mapping rules, regexes, or data structures, the performance tests must automatically
  run that exact code.

### Rule 2: Keep Only One Realistic Catalog Benchmark per Platform
To eliminate maintenance overhead and avoid flaky or redundant tests:
- **Each platform test class must have only ONE benchmark**: A realistic catalog workload (typically 1,000,000
  episodes across ~1,000 distinct shows).
- **The realistic dataset must reflect production variety**: Mix standard episodes, bonus/PV content,
  movies/specials, and excluded content (e.g. live-action or unsupported genres).
- **Do not write redundant microbenchmarks or network/cache tests per platform**: Network caching is already
  tested on `SmartHttpClient`. A single realistic catalog benchmark tests filtering, title cleaning, show
  caching, and memory churn in a single pass.

### Rule 3: Track CPU, Memory, and Bottlenecks
The benchmark automatically measures:
- **Wall-clock duration & throughput** (`items/sec`)
- **Thread CPU time & utilization %** (identifies CPU-bound string/regex crunching vs I/O waits)
- **Allocated bytes & bytes/item** (identifies object churn)
- **GC collections & pause times** (detects GC pressure exceeding 15% of execution)

### Rule 4: Adhere to Testing & Style Guidelines
- **Directory structure**: Place files directly under `src/perfTest/kotlin/perf/` and
  `src/perfTest/kotlin/platforms/impl/` without creating prefix directories for `fr/shikkanime/jobs/`.
- **Test naming**: Name test methods as complete English sentences enclosed in backticks.
- **Given / When / Then**: Delineate every test method with clear section comments.
- **Line length**: Strictly keep all lines <= 110 characters.
- **No KDoc on variables or properties**: Never add KDoc or inline comments on constants, variables, or fields.
- **Full KDoc on methods**: Every helper or non-test method must have complete KDoc with `@param`, `@return`,
  and `@see`.
- **`if` and `else` layout**: Place `if` and `else` branches on distinct lines (braces `{}` are not mandatory).
- **Dynamic getter/setter**: Place `get()` / `set()` on new indented lines.
- **Nested classes layout**: Place nested classes (other than test classes) at the beginning of the parent class.

---

## 4. How to Add a New Platform Benchmark

```kotlin
@DisplayName("tests for MyPlatform performance")
class MyPlatformPerformanceTest : BasePlatformPerformanceTest() {

    /**
     * Instantiates a mock [SmartHttpClient] with a mock engine for standalone platform testing.
     *
     * @return A configured [SmartHttpClient] instance.
     * @see [SmartHttpClient]
     * @see [MyPlatform]
     */
    private fun createMockClient(): SmartHttpClient {
        val mockEngine = MockEngine { respondOk("{}") }
        return SmartHttpClient(client = HttpClient(mockEngine))
    }

    @Test
    @DisplayName("should convert one million episodes of a realistic catalog within budget")
    fun `should convert one million episodes of a realistic catalog within budget`() {
        // Given
        val rawItems = generateRealisticDataset(1_000_000)
        val platform = MyPlatform(createMockClient())

        // When & Then
        measureAndAssert(
            label = "benchmark-myplatform-realistic-catalog",
            itemCount = 1_000_000,
            expectedOutputSize = 900_000
        ) {
            platform.convertVideos(rawItems, Country.FR)
        }
    }
}
```
