package fr.shikkanime.jobs.perf

import java.util.Locale
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Base performance test harness providing standardized profiling and regression guards.
 *
 * @see [PerfProfiler]
 * @see [PerfBudget]
 * @see [PerfReporter]
 * @see [PerfMeasurement]
 */
abstract class BasePlatformPerformanceTest {

    /**
     * Measures synchronous execution of [block] and asserts that throughput and memory stay within budget.
     *
     * @param label A descriptive identifier for the benchmark.
     * @param itemCount The number of items processed during this benchmark run.
     * @param expectedOutputSize Optional expected collection size to verify output correctness.
     * @param throughputFloor Minimum allowed items per second.
     * @param allocationCeiling Maximum allowed allocated bytes.
     * @param block The synchronous benchmark execution lambda.
     * @return A [Pair] containing the benchmark result and the recorded [PerfMeasurement].
     * @see [PerfProfiler]
     * @see [PerfBudget]
     * @see [PerfMeasurement]
     */
    protected fun <T> measureAndAssert(
        label: String,
        itemCount: Int,
        expectedOutputSize: Int? = null,
        throughputFloor: Double = PerfBudget.throughputFloor(),
        allocationCeiling: Long = PerfBudget.allocationCeiling(itemCount),
        block: () -> T
    ): Pair<T, PerfMeasurement> {
        var output: T? = null
        val measurement = PerfProfiler.measure(itemCount) {
            output = block()
        }

        println(PerfReporter.formatReport(label, measurement, allocationCeiling))
        assertMeasurement(label, measurement, throughputFloor, allocationCeiling, expectedOutputSize, output)

        @Suppress("UNCHECKED_CAST")
        return (output as T) to measurement
    }

    /**
     * Measures suspending execution of [block] and asserts that throughput and memory stay within budget.
     *
     * @param label A descriptive identifier for the benchmark.
     * @param itemCount The number of items processed during this benchmark run.
     * @param expectedOutputSize Optional expected collection size to verify output correctness.
     * @param throughputFloor Minimum allowed items per second.
     * @param allocationCeiling Maximum allowed allocated bytes.
     * @param block The suspending benchmark execution lambda.
     * @return A [Pair] containing the benchmark result and the recorded [PerfMeasurement].
     * @see [PerfProfiler]
     * @see [PerfBudget]
     * @see [PerfMeasurement]
     */
    protected suspend fun <T> measureAndAssertSuspend(
        label: String,
        itemCount: Int,
        expectedOutputSize: Int? = null,
        throughputFloor: Double = PerfBudget.throughputFloor(),
        allocationCeiling: Long = PerfBudget.allocationCeiling(itemCount),
        block: suspend () -> T
    ): Pair<T, PerfMeasurement> {
        var output: T? = null
        val measurement = PerfProfiler.measureSuspend(itemCount) {
            output = block()
        }

        println(PerfReporter.formatReport(label, measurement, allocationCeiling))
        assertMeasurement(label, measurement, throughputFloor, allocationCeiling, expectedOutputSize, output)

        @Suppress("UNCHECKED_CAST")
        return (output as T) to measurement
    }

    /**
     * Validates that the recorded [measurement] satisfies the throughput floor and allocation ceiling.
     *
     * @param label Benchmark identifier for error messages.
     * @param measurement Recorded performance metrics.
     * @param throughputFloor Required minimum items per second.
     * @param allocationCeiling Allowed maximum bytes allocated.
     * @param expectedOutputSize Expected output size if applicable.
     * @param output The output produced by the benchmark.
     * @see [PerfMeasurement]
     * @see [PerfBudget]
     */
    private fun <T> assertMeasurement(
        label: String,
        measurement: PerfMeasurement,
        throughputFloor: Double,
        allocationCeiling: Long,
        expectedOutputSize: Int?,
        output: T?
    ) {
        val actualThroughput = String.format(Locale.ROOT, "%.0f", measurement.throughputPerSecond)
        val expectedFloor = String.format(Locale.ROOT, "%.0f", throughputFloor)
        assertTrue(
            measurement.throughputPerSecond >= throughputFloor,
            "[$label] Throughput $actualThroughput items/s is below floor $expectedFloor items/s"
        )

        val actualMb = String.format(Locale.ROOT, "%.1f", measurement.allocatedMb)
        val ceilingMb = String.format(Locale.ROOT, "%.1f", allocationCeiling / (1024.0 * 1024.0))
        assertTrue(
            measurement.allocatedBytes <= allocationCeiling,
            "[$label] Allocation $actualMb MB exceeds ceiling $ceilingMb MB (memory regression)"
        )

        if (expectedOutputSize != null) {
            val actualSize = when (output) {
                is Collection<*> -> output.size
                is Array<*> -> output.size
                else -> null
            }
            assertEquals(
                expectedOutputSize,
                actualSize,
                "[$label] Output size mismatch"
            )
        }
    }
}
