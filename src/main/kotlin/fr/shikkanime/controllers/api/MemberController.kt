package fr.shikkanime.controllers.api

import com.google.inject.Inject
import fr.shikkanime.dtos.GenericDto
import fr.shikkanime.dtos.MessageDto
import fr.shikkanime.entities.Member
import fr.shikkanime.services.MemberFollowAnimeService
import fr.shikkanime.services.MemberFollowEpisodeService
import fr.shikkanime.services.MemberService
import fr.shikkanime.services.caches.MemberCacheService
import fr.shikkanime.utils.InvalidationService
import fr.shikkanime.utils.StringUtils
import fr.shikkanime.utils.routes.*
import fr.shikkanime.utils.routes.method.Delete
import fr.shikkanime.utils.routes.method.Get
import fr.shikkanime.utils.routes.method.Post
import fr.shikkanime.utils.routes.method.Put
import fr.shikkanime.utils.routes.param.BodyParam
import fr.shikkanime.utils.routes.param.HttpHeader
import fr.shikkanime.utils.routes.param.QueryParam
import io.ktor.http.content.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import java.util.*

@Controller("/api/v1/members")
class MemberController : HasPageableRoute() {
    @Inject private lateinit var memberService: MemberService
    @Inject private lateinit var memberCacheService: MemberCacheService
    @Inject private lateinit var memberFollowAnimeService: MemberFollowAnimeService
    @Inject private lateinit var memberFollowEpisodeService: MemberFollowEpisodeService

    @Path("/register")
    @Post
    private fun registerMember(): Response {
        var identifier: String

        do {
            identifier = StringUtils.generateRandomString(12)
        } while (memberService.findByIdentifier(identifier) != null)

        memberService.register(identifier)
        InvalidationService.invalidate(Member::class.java)
        return Response.created(mapOf("identifier" to identifier))
    }

    @Path("/login")
    @Post
    private fun loginMember(
        @HttpHeader("X-App-Version") appVersion: String?,
        @HttpHeader("X-Device") device: String?,
        @HttpHeader("X-Locale") locale: String?,
        @BodyParam identifier: String
    ) = memberService.login(identifier, appVersion, device, locale)?.let(Response::ok) ?: runBlocking {
        delay(1000)
        Response.notFound(MessageDto.error("Member not found"))
    }

    @Path("/associate-email")
    @Post
    @JWTAuthenticated
    private fun associateEmail(
        @JWTUser memberUuid: UUID,
        @BodyParam email: String
    ): Response {
        if (!StringUtils.isValidEmail(email))
            return Response.badRequest(MessageDto.error("Invalid email"))
        if (memberService.findByEmail(email) != null)
            return Response.conflict(MessageDto.error("Email already associated to an account"))

        return Response.created(GenericDto(memberService.associateEmail(memberUuid, email)))
    }

    @Path("/forgot-identifier")
    @Post
    @JWTAuthenticated
    private fun forgotIdentifier(
        @JWTUser memberUuid: UUID,
        @BodyParam email: String
    ): Response {
        if (!StringUtils.isValidEmail(email))
            return Response.badRequest(MessageDto.error("Invalid email"))
        if (memberCacheService.find(memberUuid)!!.email == email)
            Response.badRequest(MessageDto.error("Email already associated to your account"))

        val member = memberService.findByEmail(email)

        if (member == null) {
            runBlocking { delay(1000) }
            return Response.badRequest(MessageDto.error("Email not associated to any account"))
        }

        return Response.created(GenericDto(memberService.forgotIdentifier(member)))
    }

    @Path("/animes")
    @Put
    @JWTAuthenticated
    private fun followAnime(
        @JWTUser memberUuid: UUID,
        @BodyParam anime: GenericDto
    ) = memberFollowAnimeService.follow(memberUuid, anime)

    @Path("/animes")
    @Delete
    @JWTAuthenticated
    private fun unfollowAnime(
        @JWTUser memberUuid: UUID,
        @BodyParam anime: GenericDto
    ) = memberFollowAnimeService.unfollow(memberUuid, anime)

    @Path("/follow-all-episodes")
    @Put
    @JWTAuthenticated
    private fun followAllEpisodes(
        @JWTUser memberUuid: UUID,
        @BodyParam anime: GenericDto
    ) = memberFollowEpisodeService.followAll(memberUuid, anime)

    @Path("/episodes")
    @Put
    @JWTAuthenticated
    private fun followEpisode(
        @JWTUser memberUuid: UUID,
        @BodyParam episode: GenericDto
    ) = memberFollowEpisodeService.follow(memberUuid, episode)

    @Path("/episodes")
    @Delete
    @JWTAuthenticated
    private fun unfollowEpisode(
        @JWTUser memberUuid: UUID,
        @BodyParam episode: GenericDto
    ) = memberFollowEpisodeService.unfollow(memberUuid, episode)

    @Path("/image")
    @Post
    @JWTAuthenticated
    private fun uploadProfileImage(
        @JWTUser memberUuid: UUID,
        @BodyParam multiPartData: MultiPartData
    ): Response {
        try {
            runBlocking { memberService.changeProfileImage(memberCacheService.find(memberUuid)!!, multiPartData) }
        } catch (e: Exception) {
            return Response.badRequest(MessageDto.error(e.message ?: "Invalid file format"))
        }

        InvalidationService.invalidate(Member::class.java)
        return Response.ok()
    }

    @Path("/refresh")
    @Get
    @JWTAuthenticated
    private fun getRefreshMember(
        @JWTUser memberUuid: UUID,
        @QueryParam("limit", "9") limitParam: Int
    ): Response {
        val (_, limit, _) = pageableRoute(null, limitParam, null, null)
        return Response.ok(memberCacheService.getRefreshMember(memberUuid, limit) ?: return Response.notFound())
    }
}