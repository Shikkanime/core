package fr.shikkanime.jobs.perf

/**
 * Performance budget definition and baseline reference thresholds for platform benchmarks.
 *
 * @see [BasePlatformPerformanceTest]
 * @see [PerfMeasurement]
 * @see [PerfReporter]
 */
object PerfBudget {
    const val REFERENCE_EPISODES_PER_SECOND = 150_000.0
    const val REFERENCE_ALLOCATION_BYTES_PER_EPISODE = 3_500.0
    const val BUDGET_MULTIPLIER = 3.0

    /**
     * Calculates the minimum acceptable throughput floor in items per second.
     *
     * @return The throughput floor in items per second.
     * @see [BasePlatformPerformanceTest]
     */
    fun throughputFloor(): Double =
        REFERENCE_EPISODES_PER_SECOND / BUDGET_MULTIPLIER

    /**
     * Calculates the maximum acceptable memory allocation ceiling for in-memory mapping operations.
     *
     * @param episodeCount The workload size representing the number of processed episodes.
     * @return The allocation ceiling in bytes.
     * @see [BasePlatformPerformanceTest]
     */
    fun allocationCeiling(episodeCount: Int): Long =
        (REFERENCE_ALLOCATION_BYTES_PER_EPISODE * episodeCount * BUDGET_MULTIPLIER).toLong()
}
