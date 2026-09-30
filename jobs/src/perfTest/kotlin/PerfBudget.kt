package fr.shikkanime.jobs.perf

import com.sun.management.ThreadMXBean
import java.lang.management.ManagementFactory

/**
 * Budget guard for the performance benchmarks.
 *
 * Wall-clock thresholds alone are too flaky on shared CI runners, so each budget is
 * expressed as a ratio of a measured reference value. The ratio is intentionally loose
 * (see [BUDGET_MULTIPLIER]): its purpose is to catch a real regression — an accidental
 * O(n^2) loop, a lost cache, a removed memoization — not to police microseconds.
 *
 * Allocation is measured instead of heap delta: [ThreadMXBean.getThreadAllocatedBytes]
 * counts bytes actually allocated by the thread, so it does not depend on GC timing and
 * is stable across machines.
 */
object PerfBudget {

    /**
     * Reference values measured on the baseline (1,000,000 videos, see
     * AnimationDigitalNetworkPlatformPerformanceTest). Ratios are compared against
     * these, never against absolute time.
     *
     * Throughput reference is the SLOWEST of the three mapping benchmarks (distinct anime,
     * 1M distinct shows), so the floor applies uniformly. Allocation reference is the
     * HIGHEST observed per-episode figure (3088 B/episode, distinct anime) so the ceiling
     * is never violated by a legitimate run.
     */
    const val REFERENCE_EPISODES_PER_SECOND = 150_000.0
    const val REFERENCE_ALLOCATION_BYTES_PER_EPISODE = 3_100.0

    /**
     * How much slower / heavier than the reference a run may be before failing.
     * 3x leaves room for noisy shared runners while still failing loudly on an
     * order-of-magnitude regression.
     */
    const val BUDGET_MULTIPLIER = 3.0

    /**
     * Minimum acceptable speedup for the two micro-optimisations under test. These are
     * assertions about the OPTIMISATION still paying off, not about absolute speed, so
     * they are machine-independent: a run that is uniformly slow still passes them.
     */
    const val MIN_REGEX_SPEEDUP = 1.3
    const val MIN_MAP_PRESIZE_SPEEDUP = 1.3
    const val MIN_CACHE_SPEEDUP = 10.0

    private val threadMxBean: ThreadMXBean by lazy {
        (ManagementFactory.getThreadMXBean() as ThreadMXBean).apply {
            if (!isThreadAllocatedMemorySupported) {
                throw IllegalStateException("Thread allocation measurement is not supported on this JVM")
            }
            isThreadAllocatedMemoryEnabled = true
        }
    }

    private fun allocatedBytes(): Long =
        threadMxBean.getThreadAllocatedBytes(Thread.currentThread().id)

    /**
     * Runs [block], returning its duration in nanoseconds and the bytes allocated by
     * the current thread while it ran.
     */
    fun measure(block: () -> Unit): Measurement {
        // Force a collection first so a previous test's garbage is not attributed here.
        System.gc()
        Thread.sleep(100)
        val allocatedBefore = allocatedBytes()
        val startNanos = System.nanoTime()
        block()
        val durationNanos = System.nanoTime() - startNanos
        return Measurement(durationNanos, allocatedBytes() - allocatedBefore)
    }

    data class Measurement(val durationNanos: Long, val allocatedBytes: Long) {
        val durationMs: Double get() = durationNanos / 1_000_000.0

        /** Number of items processed per second. [itemCount] is the workload size, not a duration. */
        fun throughputPerSecond(itemCount: Int): Double =
            if (durationNanos == 0L) 0.0 else itemCount.toDouble() * 1_000_000_000.0 / durationNanos
    }

    /**
     * Returns the throughput floor in episodes per second for [episodeCount] items.
     * Scales the reference linearly so a smaller workload is not held to a rate it
     * cannot physically reach.
     */
    fun throughputFloor(episodeCount: Int): Double =
        REFERENCE_EPISODES_PER_SECOND * episodeCount / 1_000_000.0 / BUDGET_MULTIPLIER

    /**
     * Returns the allocation ceiling in bytes for [episodeCount] items.
     */
    fun allocationCeiling(episodeCount: Int): Long =
        (REFERENCE_ALLOCATION_BYTES_PER_EPISODE * episodeCount * BUDGET_MULTIPLIER).toLong()
}
