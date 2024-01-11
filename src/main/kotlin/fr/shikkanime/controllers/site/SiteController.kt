package fr.shikkanime.controllers.site

import fr.shikkanime.entities.enums.Link
import fr.shikkanime.utils.routes.Controller
import fr.shikkanime.utils.routes.Path
import fr.shikkanime.utils.routes.Response
import fr.shikkanime.utils.routes.method.Get

@Controller("/")
class SiteController {
    @Path
    @Get
    private fun home(): Response {
        return Response.template(Link.HOME)
    }
}