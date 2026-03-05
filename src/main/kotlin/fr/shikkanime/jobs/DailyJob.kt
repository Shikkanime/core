package fr.shikkanime.jobs

import com.google.inject.Inject
import fr.shikkanime.services.AttachmentService
import fr.shikkanime.services.MetricService
import fr.shikkanime.services.ProfilingService
import java.time.ZonedDateTime

class DailyJob : AbstractJob {
    @Inject private lateinit var metricService: MetricService
    @Inject private lateinit var attachmentService: AttachmentService
    @Inject private lateinit var profilingService: ProfilingService

    override suspend fun run() {
        metricService.deleteAllBefore(ZonedDateTime.now().minusWeeks(1))
        attachmentService.cleanUnusedAttachments()
        profilingService.dumpAndRestart()
        profilingService.cleanOldReports()
    }
}