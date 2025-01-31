package fr.shikkanime.controllers.api

import com.google.inject.Inject
import fr.shikkanime.converters.AbstractConverter
import fr.shikkanime.dtos.AllFollowedEpisodeDto
import fr.shikkanime.dtos.GenericDto
import fr.shikkanime.dtos.TraceActionDto
import fr.shikkanime.dtos.member.RefreshMemberDto
import fr.shikkanime.entities.Member
import fr.shikkanime.services.MemberFollowAnimeService
import fr.shikkanime.services.MemberFollowEpisodeService
import fr.shikkanime.services.MemberService
import fr.shikkanime.services.caches.MemberCacheService
import fr.shikkanime.utils.MapCache
import fr.shikkanime.utils.StringUtils
import fr.shikkanime.utils.routes.*
import fr.shikkanime.utils.routes.method.Delete
import fr.shikkanime.utils.routes.method.Get
import fr.shikkanime.utils.routes.method.Post
import fr.shikkanime.utils.routes.method.Put
import fr.shikkanime.utils.routes.openapi.OpenAPI
import fr.shikkanime.utils.routes.openapi.OpenAPIResponse
import fr.shikkanime.utils.routes.param.BodyParam
import fr.shikkanime.utils.routes.param.PathParam
import fr.shikkanime.utils.routes.param.QueryParam
import io.ktor.http.content.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import java.time.LocalDate
import java.util.*

@Controller("/api/v1/members")
class MemberController : HasPageableRoute() {
    @Inject
    private lateinit var memberService: MemberService

    @Inject
    private lateinit var memberCacheService: MemberCacheService

    @Inject
    private lateinit var memberFollowAnimeService: MemberFollowAnimeService

    @Inject
    private lateinit var memberFollowEpisodeService: MemberFollowEpisodeService

    @Path
    @Get
    @AdminSessionAuthenticated
    @OpenAPI(hidden = true)
    private fun getMembers(
        @QueryParam("page", description = "Page number for pagination")
        pageParam: Int?,
        @QueryParam("limit", description = "Number of items per page. Must be between 1 and 30", example = "15")
        limitParam: Int?,
    ): Response {
        val (page, limit, _) = pageableRoute(pageParam, limitParam, null, null)
        return Response.ok(memberService.findAllWithLastLogin(page, limit))
    }

    @Path("/{memberUuid}/login-activities")
    @Get
    @AdminSessionAuthenticated
    @OpenAPI(hidden = true)
    private fun getMemberLoginActivities(
        @PathParam("memberUuid", description = "Member UUID")
        memberUuid: UUID
    ): Response {
        val now = LocalDate.now()
        val after = now.minusMonths(1)
        val actions = memberService.findMemberLoginActivities(memberUuid, after)

        return Response.ok(
            after.datesUntil(now.plusDays(1))
                .toList()
                .associateWith { date -> AbstractConverter.convert(actions.filter { traceAction -> traceAction.actionDateTime!!.toLocalDate() == date }, TraceActionDto::class.java) }
        )
    }

    @Path("/{memberUuid}/follow-anime-activities")
    @Get
    @AdminSessionAuthenticated
    @OpenAPI(hidden = true)
    private fun getMemberFollowAnimeActivities(
        @PathParam("memberUuid", description = "Member UUID")
        memberUuid: UUID
    ): Response {
        val now = LocalDate.now()
        val after = now.minusMonths(1)
        val actions = memberService.findMemberFollowAnimeActivities(memberUuid, after)

        return Response.ok(
            after.datesUntil(now.plusDays(1))
                .toList()
                .associateWith { date -> AbstractConverter.convert(actions.filter { traceAction -> traceAction.actionDateTime!!.toLocalDate() == date }, TraceActionDto::class.java) }
        )
    }

    @Path("/{memberUuid}/follow-episode-activities")
    @Get
    @AdminSessionAuthenticated
    @OpenAPI(hidden = true)
    private fun getMemberFollowEpisodeActivities(
        @PathParam("memberUuid", description = "Member UUID")
        memberUuid: UUID
    ): Response {
        val now = LocalDate.now()
        val after = now.minusMonths(1)
        val actions = memberService.findMemberFollowEpisodeActivities(memberUuid, after)

        return Response.ok(
            after.datesUntil(now.plusDays(1))
                .toList()
                .associateWith { date -> AbstractConverter.convert(actions.filter { traceAction -> traceAction.actionDateTime!!.toLocalDate() == date }, TraceActionDto::class.java) }
        )
    }

    @Path("/register")
    @Post
    @OpenAPI(
        description = "Register member",
        responses = [
            OpenAPIResponse(201, "Member registered", Map::class),
        ]
    )
    private fun registerMember(): Response {
        var identifier: String

        do {
            identifier = StringUtils.generateRandomString(12)
        } while (memberService.findByIdentifier(identifier) != null)

        memberService.register(identifier)
        MapCache.invalidate(Member::class.java)
        return Response.created(mapOf("identifier" to identifier))
    }

    @Path("/login")
    @Post
    @OpenAPI(
        description = "Login member",
        responses = [
            OpenAPIResponse(200, "Member logged in"),
        ]
    )
    private fun loginMember(@BodyParam identifier: String): Response {
        return Response.ok(memberService.login(identifier) ?: return runBlocking {
            delay(1000)
            Response.notFound()
        })
    }

    @Path("/associate-email")
    @Post
    @JWTAuthenticated
    @OpenAPI(
        description = "Associate email to member",
        responses = [
            OpenAPIResponse(201, "Member action created", GenericDto::class),
            OpenAPIResponse(401, "Unauthorized"),
        ],
        security = true
    )
    private fun associateEmail(@JWTUser memberUuid: UUID, @BodyParam email: String): Response {
        // Verify email
        if (!StringUtils.isValidEmail(email)) {
            return Response.badRequest("Invalid email")
        }

        if (memberService.findByEmail(email) != null) {
            return Response.conflict("Email already associated to an account")
        }

        return Response.created(GenericDto(memberService.associateEmail(memberUuid, email)))
    }

    @Path("/forgot-identifier")
    @Post
    @JWTAuthenticated
    @OpenAPI(
        description = "Forgot identifier",
        responses = [
            OpenAPIResponse(200, "Code to reset identifier sent", GenericDto::class),
            OpenAPIResponse(401, "Unauthorized"),
        ],
        security = true
    )
    private fun forgotIdentifier(@JWTUser memberUuid: UUID, @BodyParam email: String): Response {
        // Verify email
        if (!StringUtils.isValidEmail(email)) {
            return Response.badRequest("Invalid email")
        }

        if (memberCacheService.find(memberUuid)!!.email == email) {
            return Response.badRequest("Email already associated to your account")
        }

        val findByEmail = memberService.findByEmail(email)

        if (findByEmail == null) {
            runBlocking { delay(1000) }
            return Response.badRequest("Email not associated to any account")
        }

        return Response.created(GenericDto(memberService.forgotIdentifier(findByEmail)))
    }


    @Path("/animes")
    @Put
    @JWTAuthenticated
    @OpenAPI(
        description = "Follow an anime",
        responses = [
            OpenAPIResponse(200, "Anime followed successfully"),
            OpenAPIResponse(401, "Unauthorized")
        ],
        security = true
    )
    private fun followAnime(@JWTUser memberUuid: UUID, @BodyParam anime: GenericDto): Response {
        return memberFollowAnimeService.follow(memberUuid, anime)
    }

    @Path("/animes")
    @Delete
    @JWTAuthenticated
    @OpenAPI(
        description = "Unfollow an anime",
        responses = [
            OpenAPIResponse(200, "Anime unfollowed successfully"),
            OpenAPIResponse(401, "Unauthorized")
        ],
        security = true
    )
    private fun unfollowAnime(@JWTUser memberUuid: UUID, @BodyParam anime: GenericDto): Response {
        return memberFollowAnimeService.unfollow(memberUuid, anime)
    }

    @Path("/follow-all-episodes")
    @Put
    @JWTAuthenticated
    @OpenAPI(
        description = "Follow all episodes of an anime",
        responses = [
            OpenAPIResponse(200, "Episodes followed successfully", AllFollowedEpisodeDto::class),
            OpenAPIResponse(401, "Unauthorized")
        ],
        security = true
    )
    private fun followAllEpisodes(@JWTUser memberUuid: UUID, @BodyParam anime: GenericDto): Response {
        return memberFollowEpisodeService.followAll(memberUuid, anime)
    }

    @Path("/episodes")
    @Put
    @JWTAuthenticated
    @OpenAPI(
        description = "Follow an episode",
        responses = [
            OpenAPIResponse(200, "Episode followed successfully"),
            OpenAPIResponse(401, "Unauthorized")
        ],
        security = true
    )
    private fun followEpisode(@JWTUser memberUuid: UUID, @BodyParam episode: GenericDto): Response {
        return memberFollowEpisodeService.follow(memberUuid, episode)
    }

    @Path("/episodes")
    @Delete
    @JWTAuthenticated
    @OpenAPI(
        description = "Unfollow an episode",
        responses = [
            OpenAPIResponse(200, "Episode unfollowed successfully"),
            OpenAPIResponse(401, "Unauthorized")
        ],
        security = true
    )
    private fun unfollowEpisode(@JWTUser memberUuid: UUID, @BodyParam episode: GenericDto): Response {
        return memberFollowEpisodeService.unfollow(memberUuid, episode)
    }

    @Path("/image")
    @Post
    @JWTAuthenticated
    @OpenAPI(
        description = "Upload an profile image",
        responses = [
            OpenAPIResponse(200, "Profile image uploaded successfully"),
            OpenAPIResponse(400, "Invalid file format"),
            OpenAPIResponse(401, "Unauthorized")
        ],
        security = true
    )
    private fun uploadProfileImage(@JWTUser memberUuid: UUID, @BodyParam multiPartData: MultiPartData): Response {
        try {
            runBlocking { memberService.changeProfileImage(memberCacheService.find(memberUuid)!!, multiPartData) }
        } catch (e: Exception) {
            return Response.badRequest(e.message ?: "Invalid file format")
        }

        MapCache.invalidate(Member::class.java)
        return Response.ok()
    }

    @Path("/refresh")
    @Get
    @JWTAuthenticated
    @OpenAPI(
        description = "Get member data after a watchlist modification",
        responses = [
            OpenAPIResponse(200, "Member data refreshed", RefreshMemberDto::class),
            OpenAPIResponse(401, "Unauthorized")
        ],
        security = true
    )
    private fun getRefreshMember(
        @JWTUser
        memberUuid: UUID,
        @QueryParam("limit", description = "Number of items per page. Must be between 1 and 30", example = "9")
        limitParam: Int?,
    ): Response {
        val (_, limit, _) = pageableRoute(null, limitParam, null, null, defaultLimit = 9)
        return Response.ok(memberCacheService.getRefreshMember(memberUuid, limit) ?: return Response.notFound())
    }
}