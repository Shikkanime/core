package fr.shikkanime.jobs

import com.google.inject.Inject
import fr.shikkanime.services.ProfilingService

class PerformanceJob : AbstractJob {
    @Inject private lateinit var profilingService: ProfilingService

    override suspend fun run() {
        profilingService.flushMetrics()
    }
}
