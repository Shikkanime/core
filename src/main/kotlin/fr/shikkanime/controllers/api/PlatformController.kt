package fr.shikkanime.controllers.api

import fr.shikkanime.dtos.PlatformDto
import fr.shikkanime.entities.enums.Platform
import fr.shikkanime.utils.routes.Controller
import fr.shikkanime.utils.routes.Path
import fr.shikkanime.utils.routes.Response
import fr.shikkanime.utils.routes.method.Get

@Controller("/api/v1/platforms")
class PlatformController {
    @Path
    @Get
    fun getAll(): Response {
        return Response.ok(Platform.entries
            .sortedBy { it.name }
            .map { platform ->
                PlatformDto(
                    id = platform.name,
                    name = platform.platformName,
                    url = platform.url,
                    image = platform.image
                )
            })
    }
}