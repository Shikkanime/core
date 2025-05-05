package fr.shikkanime.controllers.api.v2

import com.google.inject.Inject
import fr.shikkanime.dtos.LoginDto
import fr.shikkanime.services.MemberService
import fr.shikkanime.utils.routes.Controller
import fr.shikkanime.utils.routes.HasPageableRoute
import fr.shikkanime.utils.routes.Path
import fr.shikkanime.utils.routes.Response
import fr.shikkanime.utils.routes.method.Post
import fr.shikkanime.utils.routes.param.BodyParam
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking

@Deprecated("Use /api/v1/members instead with headers")
@Controller("/api/v2/members")
class MemberController : HasPageableRoute() {
    @Inject private lateinit var memberService: MemberService

    @Path("/login")
    @Post
    private fun loginMember(
        @BodyParam loginDto: LoginDto
    ): Response {
        return Response.ok(memberService.login(loginDto.identifier, loginDto.appVersion, loginDto.device, loginDto.locale) ?: return runBlocking {
            delay(1000)
            Response.notFound()
        })
    }
}