package fr.shikkanime.controllers.api

import com.google.inject.Inject
import fr.shikkanime.repositories.RouteMetricRepository
import fr.shikkanime.utils.routes.AdminSessionAuthenticated
import fr.shikkanime.utils.routes.Controller
import fr.shikkanime.utils.routes.Path
import fr.shikkanime.utils.routes.Response
import fr.shikkanime.utils.routes.method.Get
import java.time.ZonedDateTime

@Controller("/api/v1/profiling")
class ProfilingController {
    @Inject private lateinit var routeMetricRepository: RouteMetricRepository

    @Path("/routes")
    @Get
    @AdminSessionAuthenticated
    private fun getRouteMetrics() = Response.ok(routeMetricRepository.findAllAggregatedAfter(ZonedDateTime.now().minusHours(1)))
}
