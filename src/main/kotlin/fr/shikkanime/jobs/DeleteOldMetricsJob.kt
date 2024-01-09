package fr.shikkanime.jobs

import com.google.inject.Inject
import fr.shikkanime.services.MetricService
import java.time.ZonedDateTime

class DeleteOldMetricsJob : AbstractJob() {
    @Inject
    private lateinit var metricService: MetricService

    override fun run() {
        val date = ZonedDateTime.now().minusDays(3)
        metricService.deleteAllBefore(date)
    }
}