package fr.shikkanime.controllers.admin.api

import com.google.inject.Inject
import fr.shikkanime.controllers.admin.ADMIN
import fr.shikkanime.converters.AbstractConverter
import fr.shikkanime.dtos.ConfigDto
import fr.shikkanime.entities.Config
import fr.shikkanime.services.ConfigService
import fr.shikkanime.utils.Constant
import fr.shikkanime.utils.MapCache
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
class ConfigController {
    @Inject
    private lateinit var configService: ConfigService

    @Path
    @Get
    @AdminSessionAuthenticated
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

    @Path("/{uuid}")
    @Put
    @AdminSessionAuthenticated
    private fun updateConfig(@PathParam("uuid") uuid: UUID, @BodyParam configDto: ConfigDto): Response {
        configService.update(uuid, configDto)
        MapCache.invalidate(Config::class.java)
        Constant.abstractSocialNetworks.forEach { it.logout() }
        return Response.ok(AbstractConverter.convert(configService.find(uuid), ConfigDto::class.java))
    }
}