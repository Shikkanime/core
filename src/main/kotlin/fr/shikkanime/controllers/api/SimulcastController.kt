package fr.shikkanime.controllers.api

import com.google.inject.Inject
import fr.shikkanime.services.caches.SimulcastCacheService
import fr.shikkanime.utils.routes.Controller
import fr.shikkanime.utils.routes.Path
import fr.shikkanime.utils.routes.Response
import fr.shikkanime.utils.routes.method.Get

@Controller("/api/v1/simulcasts")
class SimulcastController {
    @Inject
    private lateinit var simulcastCacheService: SimulcastCacheService

    @Path
    @Get
    private fun getAll(): Response {
        return Response.ok(simulcastCacheService.findAll())
    }
}