package fr.shikkanime.controllers.admin.api

import com.google.inject.Inject
import fr.shikkanime.controllers.admin.ADMIN
import fr.shikkanime.services.AttachmentService
import fr.shikkanime.services.MetricService
import fr.shikkanime.services.TraceActionService
import fr.shikkanime.utils.routes.Controller
import fr.shikkanime.utils.routes.Path
import fr.shikkanime.utils.routes.Response
import fr.shikkanime.utils.routes.method.Get
import fr.shikkanime.utils.routes.param.QueryParam
import java.time.LocalDate

@Controller("$ADMIN/api/analytics")
class AnalyticsController {
    @Inject private lateinit var metricService: MetricService
    @Inject private lateinit var traceActionService: TraceActionService
    @Inject private lateinit var attachmentService: AttachmentService

    @Path
    @Get
    private fun getMetrics(
        @QueryParam(defaultValue = "1") hours: Long,
        @QueryParam(defaultValue = "2") activeDays: Int,
        @QueryParam(defaultValue = "30") days: Long,
    ) = Response.ok(mapOf(
        "metrics" to metricService.findAllAfterGrouped(hours),
        "analytics" to traceActionService.getAnalyticsTraceActions(LocalDate.now().minusDays(days), activeDays),
        "attachments" to attachmentService.findAllActive()
            .groupBy { it.creationDateTime.toLocalDate().toString() }
            .mapValues { it.value.size }
            .map { mapOf("date" to it.key, "count" to it.value) }
    ))
}