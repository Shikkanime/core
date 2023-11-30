package fr.shikkanime.controllers

import fr.shikkanime.utils.routes.Path
import fr.shikkanime.utils.routes.Response
import fr.shikkanime.utils.routes.TemplateResponse
import fr.ziedelth.utils.routes.method.Get

@Path("/admin")
class AdminController : AbstractController() {
    @Path("/home")
    @Get
    private fun getWebsite(): Response {
        return TemplateResponse(
            "index.ftl",
            "Dashboard",
        )
    }
}