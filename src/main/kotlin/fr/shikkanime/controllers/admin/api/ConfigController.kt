package fr.shikkanime.controllers.admin.api

import com.google.inject.Inject
import fr.shikkanime.controllers.admin.ADMIN
import fr.shikkanime.dtos.ConfigDto
import fr.shikkanime.entities.Config
import fr.shikkanime.factories.impl.ConfigFactory
import fr.shikkanime.services.ConfigService
import fr.shikkanime.utils.Constant
import fr.shikkanime.utils.InvalidationService
import fr.shikkanime.utils.routes.AdminSessionAuthenticated
import fr.shikkanime.utils.routes.Controller
import fr.shikkanime.utils.routes.Path
import fr.shikkanime.utils.routes.Response
import fr.shikkanime.utils.routes.method.Get
import fr.shikkanime.utils.routes.method.Put
import fr.shikkanime.utils.routes.param.BodyParam
import fr.shikkanime.utils.routes.param.PathParam
import fr.shikkanime.utils.routes.param.QueryParam
import java.util.*

@Controller("$ADMIN/api/configs")
@AdminSessionAuthenticated
class ConfigController {
    @Inject private lateinit var configService: ConfigService
    @Inject private lateinit var configFactory: ConfigFactory

    @Path
    @Get
    private fun getConfigs(@QueryParam name: String?) = Response.ok(
        if (name != null) {
            configService.findAllByName(name)
        } else {
            configService.findAll()
        }.map { configFactory.toDto(it) }
    )

    @Path("/{uuid}")
    @Put
    private fun updateConfig(
        @PathParam uuid: UUID,
        @BodyParam configDto: ConfigDto
    ): Response {
        val config = configService.update(uuid, configDto) ?: return Response.notFound()
        InvalidationService.invalidate(Config::class.java)
        Constant.abstractSocialNetworks.forEach { it.logout() }
        return Response.ok(configFactory.toDto(config))
    }
}