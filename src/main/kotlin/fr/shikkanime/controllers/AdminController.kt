package fr.shikkanime.controllers

import com.google.inject.Inject
import fr.shikkanime.converters.AbstractConverter
import fr.shikkanime.dtos.MemberDto
import fr.shikkanime.entities.enums.Link
import fr.shikkanime.services.MemberService
import fr.shikkanime.utils.Constant
import fr.shikkanime.utils.routes.*
import fr.ziedelth.utils.routes.method.Get
import fr.ziedelth.utils.routes.method.Post
import io.ktor.http.*

@Controller("/admin")
class AdminController {
    @Inject
    private lateinit var memberService: MemberService

    @Path
    @Get
    private fun home(@QueryParam error: String?): Response {
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
        val user = memberService.findByUsernameAndPassword(username, password) ?: return Response.redirect("/admin?error=1")

        return Response.redirect("/admin/dashboard", AbstractConverter.convert(user, MemberDto::class.java))
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
        return Response.template(
            "admin/dashboard.ftl",
            Link.DASHBOARD.label,
        )
    }

    @Path("/platforms")
    @Get
    @AdminSessionAuthenticated
    private fun getPlatforms(): Response {
        return Response.template(
            "admin/platforms.ftl",
            Link.PLATFORMS.label,
            mutableMapOf(
                "platforms" to Constant.abstractPlatforms.toList().sortedBy { it.getPlatform().name.lowercase() })
        )
    }

    @Path("/platforms")
    @Post
    @AdminSessionAuthenticated
    private fun postPlatform(@BodyParam parameters: Parameters): Response {
        val redirectResponse = Response.redirect("/admin/platforms")

        val platformName = parameters["platform"] ?: return redirectResponse
        val abstractPlatform =
            Constant.abstractPlatforms.find { it.getPlatform().name == platformName } ?: return redirectResponse
        abstractPlatform.configuration?.of(parameters)
        abstractPlatform.saveConfiguration()
        return redirectResponse
    }

    @Path("/animes")
    @Get
    @AdminSessionAuthenticated
    private fun getAnimes(): Response {
        return Response.template(
            "admin/animes.ftl",
            Link.PLATFORMS.label,
        )
    }
}