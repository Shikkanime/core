package fr.shikkanime.controllers.api

import com.google.inject.Inject
import fr.shikkanime.dtos.simulcasts.SimulcastDto
import fr.shikkanime.services.caches.SimulcastCacheService
import fr.shikkanime.utils.routes.Controller
import fr.shikkanime.utils.routes.Path
import fr.shikkanime.utils.routes.Response
import fr.shikkanime.utils.routes.method.Get
import fr.shikkanime.utils.routes.openapi.OpenAPI
import fr.shikkanime.utils.routes.openapi.OpenAPIResponse

@Controller("/api/v1/simulcasts")
class SimulcastController {
    @Inject
    private lateinit var simulcastCacheService: SimulcastCacheService

    @Path
    @Get
    @OpenAPI(
        "Get simulcasts",
        [
            OpenAPIResponse(
                200,
                "Simulcasts found",
                Array<SimulcastDto>::class,
            ),
        ]
    )
    private fun getAll(): Response {
        return Response.ok(simulcastCacheService.findAll())
    }
}