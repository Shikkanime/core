package fr.shikkanime.controllers.api

import com.google.inject.Inject
import fr.shikkanime.dtos.MessageDto
import fr.shikkanime.entities.Member
import fr.shikkanime.services.MemberActionService
import fr.shikkanime.utils.MapCache
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
    @Inject
    private lateinit var memberActionService: MemberActionService

    @Path("/validate")
    @Post
    @JWTAuthenticated
    fun validateAction(
        @QueryParam("uuid")
        uuid: UUID?,
        @BodyParam
        action: String?
    ): Response {
        if (uuid == null) {
            return Response.badRequest(
                MessageDto(
                    MessageDto.Type.ERROR,
                    "UUID is required"
                )
            )
        }

        if (action.isNullOrEmpty()) {
            return Response.badRequest(
                MessageDto(
                    MessageDto.Type.ERROR,
                    "Action is required"
                )
            )
        }

        try {
            memberActionService.validateAction(uuid, action)
            MapCache.invalidate(Member::class.java)
            return Response.ok()
        } catch (e: Exception) {
            return Response.badRequest(
                MessageDto(
                    MessageDto.Type.ERROR,
                    e.message ?: "An error occurred"
                )
            )
        }
    }
}