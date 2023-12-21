package fr.shikkanime.jobs

import com.google.inject.Inject
import fr.shikkanime.entities.Metric
import fr.shikkanime.services.MetricService
import java.lang.management.ManagementFactory
import javax.management.Attribute
import javax.management.ObjectName

class MetricJob : AbstractJob() {
    @Inject
    private lateinit var metricService: MetricService

    override fun run() {
        metricService.save(
            Metric(
                cpuLoad = getProcessCPULoad(),
                memoryUsage = getProcessMemoryUsage(),
            )
        )
    }

    private fun getProcessCPULoad(): Double {
        val mbs = ManagementFactory.getPlatformMBeanServer()
        val name = ObjectName.getInstance("java.lang:type=OperatingSystem")
        val list = mbs.getAttributes(name, arrayOf("ProcessCpuLoad"))
        if (list.isEmpty()) return Double.NaN
        val value = (list.first() as Attribute).value as Double? ?: return Double.NaN
        if (value < 0) return Double.NaN
        return value
    }

    private fun getProcessMemoryUsage(): Long {
        return Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()
    }
}