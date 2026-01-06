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


    @Path("/metrics")
    @Get
    private fun getMetrics(@QueryParam(defaultValue = "1") hours: Long) = Response.ok(metricService.findAllAfterGrouped(hours))

    @Path("/attachments")
    @Get
    private fun getAttachments() = Response.ok(attachmentService.getAttachmentCountsByDate())

    @Path("/users")
    @Get
    private fun getUsers(
        @QueryParam(defaultValue = "2") activeDays: Int,
        @QueryParam(defaultValue = "30") days: Long,
    ) = Response.ok(traceActionService.getUserAnalytics(LocalDate.now().minusDays(days), activeDays))
}