package fr.shikkanime.controllers.api.v2

import com.google.inject.Inject
import fr.shikkanime.entities.enums.CountryCode
import fr.shikkanime.services.caches.EpisodeMappingCacheService
import fr.shikkanime.utils.TelemetryConfig
import fr.shikkanime.utils.TelemetryConfig.spanWithAttributes
import fr.shikkanime.utils.routes.Controller
import fr.shikkanime.utils.routes.HasPageableRoute
import fr.shikkanime.utils.routes.Path
import fr.shikkanime.utils.routes.Response
import fr.shikkanime.utils.routes.method.Get
import fr.shikkanime.utils.routes.param.QueryParam

@Controller("/api/v2/episode-mappings")
class EpisodeMappingController : HasPageableRoute() {
    private val tracer = TelemetryConfig.getTracer("EpisodeMappingController")

    @Inject
    private lateinit var episodeMappingCacheService: EpisodeMappingCacheService

    @Path
    @Get
    private fun getAll(
        @QueryParam("country")
        countryParam: CountryCode?,
        @QueryParam("page")
        pageParam: Int?,
        @QueryParam("limit")
        limitParam: Int?,
    ) = tracer.spanWithAttributes("GET /api/v2/episode-mappings") { span ->
        val (page, limit, _) = pageableRoute(pageParam, limitParam, null, null)
        span.setAttribute("country", (countryParam ?: CountryCode.FR).name)
        span.setAttribute("page", page.toString())
        span.setAttribute("limit", limit.toString())

        Response.ok(
            episodeMappingCacheService.findAllGroupedBy(
                countryParam ?: CountryCode.FR,
                page,
                limit,
            )
        )
    }
}