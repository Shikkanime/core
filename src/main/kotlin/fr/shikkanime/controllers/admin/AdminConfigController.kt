package fr.shikkanime.controllers.admin

import com.google.inject.Inject
import fr.shikkanime.converters.AbstractConverter
import fr.shikkanime.dtos.ConfigDto
import fr.shikkanime.entities.enums.Link
import fr.shikkanime.services.ConfigService
import fr.shikkanime.utils.Constant
import fr.shikkanime.utils.routes.AdminSessionAuthenticated
import fr.shikkanime.utils.routes.Controller
import fr.shikkanime.utils.routes.Path
import fr.shikkanime.utils.routes.Response
import fr.shikkanime.utils.routes.method.Get
import fr.shikkanime.utils.routes.method.Post
import fr.shikkanime.utils.routes.param.BodyParam
import fr.shikkanime.utils.routes.param.PathParam
import io.ktor.http.*
import java.util.*

@Controller("/admin/config")
class AdminConfigController {
    @Inject
    private lateinit var configService: ConfigService

    @Path
    @Get
    @AdminSessionAuthenticated
    private fun getConfigs(): Response {
        return Response.template(Link.CONFIG)
    }

    @Path("/{uuid}")
    @Get
    @AdminSessionAuthenticated
    private fun getConfigView(@PathParam("uuid") uuid: UUID): Response {
        val config = configService.find(uuid) ?: return Response.redirect(Link.CONFIG.href)

        return Response.template(
            "admin/config/edit.ftl",
            "Edit config",
            mutableMapOf("config" to AbstractConverter.convert(config, ConfigDto::class.java))
        )
    }

    @Path("/{uuid}")
    @Post
    @AdminSessionAuthenticated
    private fun postConfig(@PathParam("uuid") uuid: UUID, @BodyParam parameters: Parameters): Response {
        configService.update(uuid, parameters)

        Constant.abstractSocialNetworks.forEach {
            it.logout()
            it.login()
        }

        return Response.redirect(Link.CONFIG.href)
    }
}