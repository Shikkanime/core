package fr.shikkanime.controllers.admin

import fr.shikkanime.entities.enums.Link
import fr.shikkanime.entities.enums.Platform
import fr.shikkanime.utils.Constant
import fr.shikkanime.utils.LoggerFactory
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
import java.util.logging.Level

@Controller("/admin/platforms")
class AdminPlatformController {
    private val logger = LoggerFactory.getLogger(javaClass)

    @Path
    @Get
    @AdminSessionAuthenticated
    private fun getPlatforms(): Response {
        return Response.template(
            Link.PLATFORMS,
            mutableMapOf(
                "platforms" to Constant.abstractPlatforms.toList().sortedBy { it.getPlatform().name.lowercase() })
        )
    }

    @Path
    @Post
    @AdminSessionAuthenticated
    private fun postPlatform(@BodyParam parameters: Parameters): Response {
        val redirectResponse = Response.redirect(Link.PLATFORMS.href)

        val platformName = parameters["platform"] ?: return redirectResponse
        val abstractPlatform =
            Constant.abstractPlatforms.find { it.getPlatform().name == platformName } ?: return redirectResponse
        abstractPlatform.configuration?.of(parameters)
        abstractPlatform.saveConfiguration()
        abstractPlatform.reset()

        return redirectResponse
    }

    @Path("/{platform}/simulcasts")
    @Get
    @AdminSessionAuthenticated
    private fun getPlatformSimulcast(@PathParam("platform") platform: Platform): Response {
        val find = Constant.abstractPlatforms.find { it.getPlatform().name == platform.name }
            ?: return Response.redirect(Link.PLATFORMS.href)

        return Response.template(
            "admin/platforms/edit.ftl",
            platform.name,
            mutableMapOf(
                "platform" to platform,
                "simulcast_config" to find.configuration!!.newPlatformSimulcast()
            )
        )
    }

    @Path("/{platform}/simulcasts/{uuid}")
    @Get
    @AdminSessionAuthenticated
    private fun getPlatformSimulcasts(
        @PathParam("platform") platform: Platform,
        @PathParam("uuid") uuid: UUID
    ): Response {
        val find = Constant.abstractPlatforms.find { it.getPlatform().name == platform.name }
            ?: return Response.redirect(Link.PLATFORMS.href)

        return Response.template(
            "admin/platforms/edit.ftl",
            platform.name,
            mutableMapOf(
                "platform" to platform,
                "simulcast_config" to find.configuration!!.simulcasts.find { it.uuid == uuid }
            )
        )
    }

    @Path("/{platform}/simulcasts")
    @Post
    @AdminSessionAuthenticated
    private fun addPlatformSimulcast(
        @PathParam("platform") platform: Platform,
        @BodyParam parameters: Parameters
    ): Response {
        val abstractPlatform = Constant.abstractPlatforms.find { it.getPlatform().name == platform.name }
            ?: return Response.redirect(Link.PLATFORMS.href)
        val platformConfiguration = abstractPlatform.configuration!!

        val uuid = parameters["uuid"]?.let { UUID.fromString(it) }
        val simulcast =
            platformConfiguration.simulcasts.find { it.uuid == uuid } ?: platformConfiguration.newPlatformSimulcast()
        simulcast.of(parameters)

        if (simulcast.uuid == null) {
            simulcast.uuid = UUID.randomUUID()

            if (!platformConfiguration.addPlatformSimulcast(simulcast)) {
                logger.log(Level.SEVERE, "Failed to add simulcast")
            }
        }

        abstractPlatform.saveConfiguration()
        abstractPlatform.reset()

        return Response.redirect(Link.PLATFORMS.href)
    }

    @Path("/{platform}/simulcasts/{uuid}/delete")
    @Get
    @AdminSessionAuthenticated
    private fun deletePlatformSimulcast(
        @PathParam("platform") platform: Platform,
        @PathParam("uuid") uuid: UUID
    ): Response {
        val abstractPlatform = Constant.abstractPlatforms.find { it.getPlatform().name == platform.name }
            ?: return Response.redirect(Link.PLATFORMS.href)
        abstractPlatform.configuration?.simulcasts?.removeIf { it.uuid == uuid }
        abstractPlatform.saveConfiguration()
        abstractPlatform.reset()

        return Response.redirect(Link.PLATFORMS.href)
    }
}