package fr.shikkanime.controllers.admin.api

import com.google.inject.Inject
import fr.shikkanime.controllers.admin.ADMIN
import fr.shikkanime.converters.AbstractConverter
import fr.shikkanime.dtos.MetricDto
import fr.shikkanime.services.MetricService
import fr.shikkanime.utils.routes.AdminSessionAuthenticated
import fr.shikkanime.utils.routes.Controller
import fr.shikkanime.utils.routes.Path
import fr.shikkanime.utils.routes.Response
import fr.shikkanime.utils.routes.method.Get
import fr.shikkanime.utils.routes.param.QueryParam
import java.time.ZonedDateTime

@Controller("$ADMIN/api/metrics")
class MetricController {
    @Inject
    private lateinit var metricService: MetricService

    @Path
    @Get
    @AdminSessionAuthenticated
    private fun getMetrics(
        @QueryParam("hours") hours: Int?,
    ): Response {
        val xHourAgo = ZonedDateTime.now().minusHours(hours?.toLong() ?: 1)
        return Response.ok(AbstractConverter.convert(metricService.findAllAfter(xHourAgo), MetricDto::class.java))
    }
}