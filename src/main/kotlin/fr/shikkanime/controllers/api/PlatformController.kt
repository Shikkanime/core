package fr.shikkanime.controllers.api

import com.google.inject.Inject
import fr.shikkanime.entities.enums.Platform
import fr.shikkanime.factories.impl.PlatformFactory
import fr.shikkanime.utils.routes.Controller
import fr.shikkanime.utils.routes.Path
import fr.shikkanime.utils.routes.Response
import fr.shikkanime.utils.routes.method.Get

@Controller("/api/v1/platforms")
class PlatformController {
    @Inject private lateinit var platformFactory: PlatformFactory

    @Path
    @Get
    private fun getAll(): Response {
        return Response.ok(Platform.entries.map { platformFactory.toDto(it) })
    }
}