package fr.shikkanime.controllers.site

import com.google.inject.Inject
import fr.shikkanime.services.MemberActionService
import fr.shikkanime.utils.routes.Controller
import fr.shikkanime.utils.routes.Path
import fr.shikkanime.utils.routes.Response
import fr.shikkanime.utils.routes.method.Get
import fr.shikkanime.utils.routes.param.PathParam

@Controller("/")
class ShortLinkController {
    @Inject
    private lateinit var memberActionService: MemberActionService

    @Path("v/{webTokenAction}")
    @Get
    private fun validateWebToken(@PathParam("webTokenAction") webToken: String): Response {
        try {
            memberActionService.validateWebAction(webToken)
            return Response.template("/site/validateAction.ftl")
        } catch (_: Exception) {
            return Response.redirect("/")
        }
    }
}