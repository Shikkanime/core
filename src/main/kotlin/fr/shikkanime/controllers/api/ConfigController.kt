package fr.shikkanime.controllers.api

import com.google.inject.Inject
import fr.shikkanime.converters.AbstractConverter
import fr.shikkanime.dtos.ConfigDto
import fr.shikkanime.dtos.MessageDto
import fr.shikkanime.services.ConfigService
import fr.shikkanime.utils.routes.AdminSessionAuthenticated
import fr.shikkanime.utils.routes.Controller
import fr.shikkanime.utils.routes.Path
import fr.shikkanime.utils.routes.Response
import fr.shikkanime.utils.routes.method.Get
import fr.shikkanime.utils.routes.openapi.OpenAPI
import fr.shikkanime.utils.routes.openapi.OpenAPIResponse
import fr.shikkanime.utils.routes.param.QueryParam

@Controller("/api/config")
class ConfigController {
    @Inject
    private lateinit var configService: ConfigService

    @Path
    @Get
    @AdminSessionAuthenticated
    @OpenAPI(
        "Get configs",
        [
            OpenAPIResponse(
                200,
                "Configs found",
                Array<ConfigDto>::class,
            ),
            OpenAPIResponse(
                401,
                "You are not authenticated",
                MessageDto::class
            ),
        ]
    )
    private fun getConfigs(
        @QueryParam("name") nameParam: String?,
    ): Response {
        val configs = if (nameParam != null) {
            configService.findAllByName(nameParam)
        } else {
            configService.findAll()
        }

        return Response.ok(AbstractConverter.convert(configs, ConfigDto::class.java))
    }
}