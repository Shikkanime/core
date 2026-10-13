package fr.shikkanime.jobs.perf

import java.util.Locale

/**
 * Diagnostic reporter generating tabular benchmark summaries and diagnosing resource bottlenecks.
 *
 * @see [PerfMeasurement]
 * @see [BasePlatformPerformanceTest]
 */
object PerfReporter {

    /**
     * Identifies the primary resource bottlenecks based on the given execution [measurement].
     *
     * @param measurement The performance metrics snapshot.
     * @param allocationCeilingBytes Optional explicit ceiling in bytes used to calibrate memory churn threshold.
     * @return A descriptive bottleneck label or diagnostic summary string.
     * @see [PerfMeasurement]
     */
    fun diagnoseBottleneck(
        measurement: PerfMeasurement,
        allocationCeilingBytes: Long? = null
    ): String {
        val diagnoses = mutableListOf<String>()

        if (measurement.gcOverheadPercent >= 15.0) {
            val pauseStr = String.format(Locale.ROOT, "%.1f", measurement.gcOverheadPercent)
            diagnoses.add("GC PRESSURE ($pauseStr% pause overhead)")
        }

        val allocationThresholdBytesPerItem = if (allocationCeilingBytes != null && measurement.itemCount > 0)
            (allocationCeilingBytes.toDouble() / measurement.itemCount) * 0.95
        else
            4_000.0

        if (measurement.bytesPerItem > allocationThresholdBytesPerItem) {
            val churnStr = String.format(Locale.ROOT, "%.0f", measurement.bytesPerItem)
            diagnoses.add("HIGH MEMORY CHURN ($churnStr B/item)")
        }

        if (measurement.cpuUtilizationPercent >= 80.0 && measurement.gcOverheadPercent < 10.0) {
            val coreStr = String.format(Locale.ROOT, "%.1f", measurement.cpuUtilizationPercent)
            diagnoses.add("CPU-BOUND ($coreStr% core utilization)")
        } else if (measurement.cpuUtilizationPercent < 50.0 && measurement.durationMs > 200.0) {
            val cpuStr = String.format(Locale.ROOT, "%.1f", measurement.cpuUtilizationPercent)
            diagnoses.add("I/O OR SCHEDULING DELAY ($cpuStr% CPU utilization)")
        }

        if (diagnoses.isEmpty())
            diagnoses.add("BALANCED (Optimal compute and allocation distribution)")

        return diagnoses.joinToString(" | ")
    }

    /**
     * Formats a complete human-readable diagnostic report for a benchmark execution.
     *
     * @param label A descriptive identifier for the benchmark.
     * @param measurement The performance metrics snapshot.
     * @param allocationCeilingBytes Optional allocation ceiling in bytes for threshold diagnosis.
     * @return A formatted multiline ASCII report string.
     * @see [PerfMeasurement]
     */
    fun formatReport(
        label: String,
        measurement: PerfMeasurement,
        allocationCeilingBytes: Long? = null
    ): String {
        val durMs = String.format(Locale.ROOT, "%.1f", measurement.durationMs)
        val durSec = String.format(Locale.ROOT, "%.2f", measurement.durationMs / 1000.0)
        val thr = String.format(Locale.ROOT, "%.0f", measurement.throughputPerSecond)
        val allocMb = String.format(Locale.ROOT, "%.1f", measurement.allocatedMb)
        val bPerItem = String.format(Locale.ROOT, "%.0f", measurement.bytesPerItem)
        val cpuMs = String.format(Locale.ROOT, "%.1f", measurement.cpuTimeMs)
        val userCpuMs = String.format(Locale.ROOT, "%.1f", measurement.userCpuTimeMs)
        val cpuUtil = String.format(Locale.ROOT, "%.1f", measurement.cpuUtilizationPercent)
        val bottleneck = diagnoseBottleneck(measurement, allocationCeilingBytes)

        return """
            |--------------------------------------------------------------------------------
            | BENCHMARK: $label
            |--------------------------------------------------------------------------------
            |  Duration      : $durMs ms ($durSec s)
            |  Throughput    : $thr items/sec
            |  Thread CPU    : $cpuMs ms (User: $userCpuMs ms, Util: $cpuUtil%)
            |  Allocated Mem : $allocMb MB ($bPerItem B/item)
            |  GC Activity   : ${measurement.gcCountDelta} collections, ${measurement.gcTimeDeltaMs} ms pause
            |  Bottleneck    : $bottleneck
            |--------------------------------------------------------------------------------
        """.trimMargin()
    }
}
