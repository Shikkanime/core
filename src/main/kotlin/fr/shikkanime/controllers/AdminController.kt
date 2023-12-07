package fr.shikkanime.controllers

import fr.shikkanime.utils.Constant
import fr.shikkanime.utils.routes.*
import fr.ziedelth.utils.routes.method.Get
import fr.ziedelth.utils.routes.method.Post
import io.ktor.http.*

@Controller("/admin")
class AdminController {
    @Path("/dashboard")
    @Get
    private fun getDashboard(): Response {
        return TemplateResponse(
            "admin/dashboard.ftl",
            "Dashboard",
        )
    }

    @Path("/platforms")
    @Get
    private fun getPlatforms(): Response {
        return TemplateResponse(
            "admin/platforms.ftl",
            "Platforms",
            mutableMapOf("platforms" to Constant.abstractPlatforms)
        )
    }

    @Path("/platforms")
    @Post
    private fun postPlatform(@BodyParam parameters: Parameters): Response {
        val redirectResponse = RedirectResponse("/admin/platforms")

        val platformName = parameters["platform"] ?: return redirectResponse
        val abstractPlatform = Constant.abstractPlatforms.find { it.getPlatform().name == platformName } ?: return redirectResponse
        abstractPlatform.configuration?.of(parameters)
        abstractPlatform.saveConfiguration()
        return redirectResponse
    }
}