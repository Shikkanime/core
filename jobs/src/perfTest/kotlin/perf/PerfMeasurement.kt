package fr.shikkanime.jobs.perf

/**
 * Encapsulates execution metrics collected by [PerfProfiler] during a benchmark run.
 *
 * @see [PerfProfiler]
 * @see [PerfReporter]
 * @see [PerfBudget]
 */
data class PerfMeasurement(
    val durationNanos: Long,
    val cpuTimeNanos: Long,
    val userCpuTimeNanos: Long,
    val allocatedBytes: Long,
    val heapUsedDeltaBytes: Long,
    val gcCountDelta: Long,
    val gcTimeDeltaMs: Long,
    val itemCount: Int
) {
    val durationMs: Double
        get() = durationNanos / 1_000_000.0

    val cpuTimeMs: Double
        get() = cpuTimeNanos / 1_000_000.0

    val userCpuTimeMs: Double
        get() = userCpuTimeNanos / 1_000_000.0

    val allocatedMb: Double
        get() = allocatedBytes / (1024.0 * 1024.0)

    val throughputPerSecond: Double
        get() =
            if (durationNanos == 0L)
                0.0
            else
                itemCount.toDouble() * 1_000_000_000.0 / durationNanos

    val bytesPerItem: Double
        get() =
            if (itemCount <= 0)
                0.0
            else
                allocatedBytes.toDouble() / itemCount

    val cpuUtilizationPercent: Double
        get() =
            if (durationNanos == 0L)
                0.0
            else
                (cpuTimeNanos.toDouble() / durationNanos) * 100.0

    val gcOverheadPercent: Double
        get() =
            if (durationMs == 0.0)
                0.0
            else
                (gcTimeDeltaMs.toDouble() / durationMs) * 100.0
}
