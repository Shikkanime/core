package fr.shikkanime.controllers.api

import com.google.inject.Inject
import fr.shikkanime.dtos.MessageDto
import fr.shikkanime.entities.Member
import fr.shikkanime.services.MemberActionService
import fr.shikkanime.utils.MapCache
import fr.shikkanime.utils.TelemetryConfig
import fr.shikkanime.utils.TelemetryConfig.trace
import fr.shikkanime.utils.routes.Controller
import fr.shikkanime.utils.routes.JWTAuthenticated
import fr.shikkanime.utils.routes.Path
import fr.shikkanime.utils.routes.Response
import fr.shikkanime.utils.routes.method.Post
import fr.shikkanime.utils.routes.param.BodyParam
import fr.shikkanime.utils.routes.param.QueryParam
import java.util.*

@Controller("/api/v1/member-actions")
class MemberActionController {
    private val tracer = TelemetryConfig.getTracer("MemberActionController")
    @Inject private lateinit var memberActionService: MemberActionService

    @Path("/validate")
    @Post
    @JWTAuthenticated
    fun validateAction(
        @QueryParam uuid: UUID?,
        @BodyParam action: String?
    ): Response {
        if (uuid == null)
            Response.badRequest(MessageDto.error("UUID is required"))
        if (action.isNullOrEmpty())
            return Response.badRequest(MessageDto.error("Action is required"))

        try {
            tracer.trace { memberActionService.validateAction(uuid!!, action) }
            MapCache.invalidate(Member::class.java)
            return Response.ok()
        } catch (e: Exception) {
            return Response.badRequest(MessageDto.error(e.message ?: "An error occurred"))
        }
    }
}