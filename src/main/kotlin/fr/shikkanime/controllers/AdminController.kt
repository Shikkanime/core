package fr.shikkanime.controllers

import fr.shikkanime.utils.routes.Controller
import fr.shikkanime.utils.routes.Path
import fr.shikkanime.utils.routes.Response
import fr.shikkanime.utils.routes.TemplateResponse
import fr.ziedelth.utils.routes.method.Get

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
        )
    }
}