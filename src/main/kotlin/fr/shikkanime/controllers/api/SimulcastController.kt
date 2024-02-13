package fr.shikkanime.controllers.api

import com.google.inject.Inject
import fr.shikkanime.converters.AbstractConverter
import fr.shikkanime.dtos.simulcasts.SimulcastDto
import fr.shikkanime.services.SimulcastService
import fr.shikkanime.utils.routes.*
import fr.shikkanime.utils.routes.method.Get
import fr.shikkanime.utils.routes.openapi.OpenAPI
import fr.shikkanime.utils.routes.openapi.OpenAPIResponse
import io.ktor.http.*
import java.util.*

@Controller("/api/v1/simulcasts")
class SimulcastController {
    @Inject
    private lateinit var simulcastService: SimulcastService

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
        return Response.ok(AbstractConverter.convert(simulcastService.findAll(), SimulcastDto::class.java))
    }
}