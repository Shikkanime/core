package fr.shikkanime.controllers.api

import com.google.inject.Inject
import fr.shikkanime.converters.AbstractConverter
import fr.shikkanime.dtos.MessageDto
import fr.shikkanime.dtos.MetricDto
import fr.shikkanime.services.MetricService
import fr.shikkanime.utils.routes.AdminSessionAuthenticated
import fr.shikkanime.utils.routes.Controller
import fr.shikkanime.utils.routes.Path
import fr.shikkanime.utils.routes.Response
import fr.shikkanime.utils.routes.method.Get
import fr.shikkanime.utils.routes.openapi.OpenAPI
import fr.shikkanime.utils.routes.openapi.OpenAPIResponse
import fr.shikkanime.utils.routes.param.QueryParam
import java.time.ZonedDateTime

@Controller("/api/metrics")
class MetricController {
    @Inject
    private lateinit var metricService: MetricService

    @Path
    @Get
    @AdminSessionAuthenticated
    @OpenAPI(
        "Get metrics",
        [
            OpenAPIResponse(
                200,
                "Metrics found",
                Array<MetricDto>::class,
            ),
            OpenAPIResponse(
                401,
                "You are not authenticated",
                MessageDto::class
            ),
        ]
    )
    private fun getMetrics(
        @QueryParam("hours") hours: Int?,
    ): Response {
        val oneHourAgo = ZonedDateTime.now().minusHours(hours?.toLong() ?: 1)
        return Response.ok(AbstractConverter.convert(metricService.findAllAfter(oneHourAgo), MetricDto::class.java))
    }
}