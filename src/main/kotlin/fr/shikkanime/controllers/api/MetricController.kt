package fr.shikkanime.controllers.api

import com.google.inject.Inject
import fr.shikkanime.converters.AbstractConverter
import fr.shikkanime.dtos.MetricDto
import fr.shikkanime.services.MetricService
import fr.shikkanime.utils.routes.Controller
import fr.shikkanime.utils.routes.Path
import fr.shikkanime.utils.routes.Response
import fr.ziedelth.utils.routes.method.Get
import java.time.ZonedDateTime

@Controller("/api/metrics")
class MetricController {
    @Inject
    private lateinit var metricService: MetricService

    @Path
    @Get
    private fun getPlatforms(): Response {
        val oneHourAgo = ZonedDateTime.now().minusHours(1)
        return Response.ok(AbstractConverter.convert(metricService.findAllAfter(oneHourAgo), MetricDto::class.java))
    }
}