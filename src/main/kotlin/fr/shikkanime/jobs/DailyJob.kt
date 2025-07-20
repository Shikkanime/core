package fr.shikkanime.jobs

import com.google.inject.Inject
import fr.shikkanime.services.AttachmentService
import fr.shikkanime.services.MetricService
import java.time.ZonedDateTime

class DailyJob : AbstractJob {
    @Inject private lateinit var metricService: MetricService
    @Inject private lateinit var attachmentService: AttachmentService

    override fun run() {
        metricService.deleteAllBefore(ZonedDateTime.now().minusWeeks(1))
        attachmentService.cleanUnusedAttachments()
    }
}