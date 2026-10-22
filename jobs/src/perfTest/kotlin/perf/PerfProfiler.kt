package fr.shikkanime.jobs.perf

import com.sun.management.ThreadMXBean
import java.lang.management.GarbageCollectorMXBean
import java.lang.management.ManagementFactory

/**
 * JVM execution and resource profiler measuring wall-clock, CPU, memory, and GC metrics.
 *
 * @see [PerfMeasurement]
 * @see [PerfReporter]
 * @see [BasePlatformPerformanceTest]
 */
object PerfProfiler {

    private data class Baseline(
        val threadId: Long,
        val heapBefore: Long,
        val gcCountBefore: Long,
        val gcTimeBefore: Long,
        val allocBefore: Long,
        val cpuBefore: Long,
        val userCpuBefore: Long,
        val startNanos: Long
    )

    private val threadMxBean: ThreadMXBean by lazy {
        (ManagementFactory.getThreadMXBean() as ThreadMXBean).apply {
            if (isThreadCpuTimeSupported && !isThreadCpuTimeEnabled)
                isThreadCpuTimeEnabled = true
            if (isThreadAllocatedMemorySupported && !isThreadAllocatedMemoryEnabled)
                isThreadAllocatedMemoryEnabled = true
        }
    }

    private val gcBeans: List<GarbageCollectorMXBean> by lazy {
        ManagementFactory.getGarbageCollectorMXBeans()
    }

    /**
     * Calculates the currently consumed JVM heap memory in bytes.
     *
     * @return The consumed heap memory in bytes.
     * @see [Runtime]
     */
    fun getUsedHeapMemoryBytes(): Long {
        val runtime = Runtime.getRuntime()
        return runtime.totalMemory() - runtime.freeMemory()
    }

    /**
     * Retrieves cumulative GC collection count and pause duration across all JVM collectors.
     *
     * @return A [Pair] containing total collection count and total pause duration in milliseconds.
     * @see [GarbageCollectorMXBean]
     */
    fun getGarbageCollectionStats(): Pair<Long, Long> {
        var count = 0L
        var time = 0L
        for (gcBean in gcBeans) {
            val c = gcBean.collectionCount
            val t = gcBean.collectionTime
            if (c > 0)
                count += c
            if (t > 0)
                time += t
        }
        return count to time
    }

    /**
     * Triggers garbage collection cycles and pauses to minimize interference from prior allocations.
     *
     * @see [System.gc]
     */
    fun triggerGc() {
        System.gc()
        Thread.sleep(60)
        System.gc()
        Thread.sleep(60)
    }

    /**
     * Captures pre-execution baseline metrics for the current thread and JVM memory state.
     *
     * @return A captured [Baseline] snapshot.
     * @see [Baseline]
     */
    private fun captureBaseline(): Baseline {
        triggerGc()
        val threadId = Thread.currentThread().threadId()
        val heapBefore = getUsedHeapMemoryBytes()
        val (gcCountBefore, gcTimeBefore) = getGarbageCollectionStats()
        val allocBefore = threadMxBean.getThreadAllocatedBytes(threadId)
        val cpuBefore = threadMxBean.getThreadCpuTime(threadId)
        val userCpuBefore = threadMxBean.getThreadUserTime(threadId)
        val startNanos = System.nanoTime()

        return Baseline(
            threadId = threadId,
            heapBefore = heapBefore,
            gcCountBefore = gcCountBefore,
            gcTimeBefore = gcTimeBefore,
            allocBefore = allocBefore,
            cpuBefore = cpuBefore,
            userCpuBefore = userCpuBefore,
            startNanos = startNanos
        )
    }

    /**
     * Computes the difference between post-execution state and the recorded [baseline].
     *
     * @param baseline The pre-execution baseline metrics.
     * @param itemCount The number of items processed during execution.
     * @return A populated [PerfMeasurement] with differential metrics.
     * @see [Baseline]
     * @see [PerfMeasurement]
     */
    private fun buildMeasurement(baseline: Baseline, itemCount: Int): PerfMeasurement {
        val durationNanos = System.nanoTime() - baseline.startNanos
        val cpuTimeNanos = threadMxBean.getThreadCpuTime(baseline.threadId) - baseline.cpuBefore
        val userCpuTimeNanos = threadMxBean.getThreadUserTime(baseline.threadId) - baseline.userCpuBefore
        val allocAfter = threadMxBean.getThreadAllocatedBytes(baseline.threadId) - baseline.allocBefore
        val (gcCountAfter, gcTimeAfter) = getGarbageCollectionStats()
        val heapAfter = getUsedHeapMemoryBytes()

        return PerfMeasurement(
            durationNanos = durationNanos,
            cpuTimeNanos = cpuTimeNanos,
            userCpuTimeNanos = userCpuTimeNanos,
            allocatedBytes = allocAfter,
            heapUsedDeltaBytes = heapAfter - baseline.heapBefore,
            gcCountDelta = gcCountAfter - baseline.gcCountBefore,
            gcTimeDeltaMs = gcTimeAfter - baseline.gcTimeBefore,
            itemCount = itemCount
        )
    }

    /**
     * Executes the given [block] synchronously and records comprehensive execution metrics.
     *
     * @param itemCount The workload size representing the number of processed elements.
     * @param block The benchmark execution lambda to profile.
     * @return The resulting [PerfMeasurement] snapshot.
     * @see [PerfMeasurement]
     * @see [BasePlatformPerformanceTest]
     */
    fun measure(itemCount: Int = 1, block: () -> Unit): PerfMeasurement {
        val baseline = captureBaseline()
        block()
        return buildMeasurement(baseline, itemCount)
    }
}
