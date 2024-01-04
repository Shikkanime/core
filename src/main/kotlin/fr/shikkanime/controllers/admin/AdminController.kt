package fr.shikkanime.controllers.admin

import com.google.inject.Inject
import fr.shikkanime.converters.AbstractConverter
import fr.shikkanime.dtos.MemberDto
import fr.shikkanime.entities.enums.Link
import fr.shikkanime.entities.enums.Platform
import fr.shikkanime.services.MemberService
import fr.shikkanime.utils.Constant
import fr.shikkanime.utils.routes.AdminSessionAuthenticated
import fr.shikkanime.utils.routes.Controller
import fr.shikkanime.utils.routes.Path
import fr.shikkanime.utils.routes.Response
import fr.shikkanime.utils.routes.method.Get
import fr.shikkanime.utils.routes.method.Post
import fr.shikkanime.utils.routes.param.BodyParam
import fr.shikkanime.utils.routes.param.PathParam
import fr.shikkanime.utils.routes.param.QueryParam
import io.ktor.http.*
import java.util.*

@Controller("/admin")
class AdminController {
    @Inject
    private lateinit var memberService: MemberService

    @Path
    @Get
    private fun home(@QueryParam("error") error: String?): Response {
        return Response.template(
            "admin/login.ftl",
            "Login",
            if (!error.isNullOrBlank()) mutableMapOf("error" to "Invalid credentials") else mutableMapOf()
        )
    }

    @Path("/login")
    @Post
    private fun login(@BodyParam parameters: Parameters): Response {
        val username = parameters["username"] ?: return Response.redirect("/admin")
        val password = parameters["password"] ?: return Response.redirect("/admin")
        val user =
            memberService.findByUsernameAndPassword(username, password) ?: return Response.redirect("/admin?error=1")

        return Response.redirect(Link.DASHBOARD.href, AbstractConverter.convert(user, MemberDto::class.java))
    }

    @Path("/logout")
    @Get
    @AdminSessionAuthenticated
    private fun logout(): Response {
        return Response.redirect("/admin", MemberDto.empty)
    }

    @Path("/dashboard")
    @Get
    @AdminSessionAuthenticated
    private fun getDashboard(): Response {
        return Response.template(Link.DASHBOARD)
    }

    @Path("/platforms")
    @Get
    @AdminSessionAuthenticated
    private fun getPlatforms(): Response {
        return Response.template(
            Link.PLATFORMS,
            mutableMapOf(
                "platforms" to Constant.abstractPlatforms.toList().sortedBy { it.getPlatform().name.lowercase() })
        )
    }

    @Path("/platforms/{platform}/simulcasts")
    @Get
    @AdminSessionAuthenticated
    private fun getPlatforms(@PathParam("platform") platform: Platform): Response {
        val find = Constant.abstractPlatforms.find { it.getPlatform().name == platform.name } ?: return Response.redirect(Link.PLATFORMS.href)

        return Response.template(
            "admin/platform_simulcasts.ftl",
            platform.name,
            mutableMapOf(
                "platform" to platform,
                "simulcast_config" to find.configuration!!.newPlatformSimulcast()
            )
        )
    }

    @Path("/platforms/{platform}/simulcasts/{uuid}")
    @Get
    @AdminSessionAuthenticated
    private fun getPlatformSimulcasts(@PathParam("platform") platform: Platform, @PathParam("uuid") uuid: UUID): Response {
        val find = Constant.abstractPlatforms.find { it.getPlatform().name == platform.name } ?: return Response.redirect(Link.PLATFORMS.href)

        return Response.template(
            "admin/platform_simulcasts.ftl",
            platform.name,
            mutableMapOf(
                "platform" to platform,
                "simulcast_config" to find.configuration!!.simulcasts.find { it.uuid == uuid }
            )
        )
    }

    @Path("/platforms/{platform}/simulcasts")
    @Post
    @AdminSessionAuthenticated
    private fun addPlatformSimulcast(@PathParam("platform") platform: Platform, @BodyParam parameters: Parameters): Response {
        val abstractPlatform = Constant.abstractPlatforms.find { it.getPlatform().name == platform.name } ?: return Response.redirect(Link.PLATFORMS.href)
        val uuid = parameters["uuid"]?.let { UUID.fromString(it) }
        val simulcast = abstractPlatform.configuration!!.simulcasts.find { it.uuid == uuid } ?: abstractPlatform.configuration!!.newPlatformSimulcast()
        simulcast.of(parameters)

        if (uuid == null) {
            simulcast.uuid = UUID.randomUUID()
            abstractPlatform.configuration?.addPlatformSimulcast(simulcast)
        }

        abstractPlatform.saveConfiguration()
        return Response.redirect(Link.PLATFORMS.href)
    }

    @Path("/platforms/{platform}/simulcasts/{uuid}/delete")
    @Get
    @AdminSessionAuthenticated
    private fun deletePlatformSimulcast(@PathParam("platform") platform: Platform, @PathParam("uuid") uuid: UUID): Response {
        val abstractPlatform = Constant.abstractPlatforms.find { it.getPlatform().name == platform.name } ?: return Response.redirect(Link.PLATFORMS.href)
        abstractPlatform.configuration?.simulcasts?.removeIf { it.uuid == uuid }
        abstractPlatform.saveConfiguration()
        return Response.redirect(Link.PLATFORMS.href)
    }

    @Path("/platforms")
    @Post
    @AdminSessionAuthenticated
    private fun postPlatform(@BodyParam parameters: Parameters): Response {
        val redirectResponse = Response.redirect(Link.PLATFORMS.href)

        val platformName = parameters["platform"] ?: return redirectResponse
        val abstractPlatform =
            Constant.abstractPlatforms.find { it.getPlatform().name == platformName } ?: return redirectResponse
        abstractPlatform.configuration?.of(parameters)
        abstractPlatform.saveConfiguration()
        return redirectResponse
    }
}