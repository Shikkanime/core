package fr.shikkanime.controllers.api

import com.google.inject.Inject
import fr.shikkanime.dtos.AllFollowedEpisodeDto
import fr.shikkanime.dtos.GenericDto
import fr.shikkanime.dtos.member.RefreshMemberDto
import fr.shikkanime.services.ImageService
import fr.shikkanime.services.MemberFollowAnimeService
import fr.shikkanime.services.MemberFollowEpisodeService
import fr.shikkanime.services.MemberService
import fr.shikkanime.services.caches.MemberCacheService
import fr.shikkanime.utils.StringUtils
import fr.shikkanime.utils.routes.*
import fr.shikkanime.utils.routes.method.Delete
import fr.shikkanime.utils.routes.method.Get
import fr.shikkanime.utils.routes.method.Post
import fr.shikkanime.utils.routes.method.Put
import fr.shikkanime.utils.routes.openapi.OpenAPI
import fr.shikkanime.utils.routes.openapi.OpenAPIResponse
import fr.shikkanime.utils.routes.param.BodyParam
import fr.shikkanime.utils.routes.param.QueryParam
import io.ktor.http.content.*
import io.ktor.utils.io.readRemaining
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.io.readByteArray
import java.io.ByteArrayInputStream
import java.util.*
import javax.imageio.ImageIO

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
        } while (memberCacheService.findByIdentifier(identifier) != null)

        memberService.save(identifier)
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
        return Response.ok(memberCacheService.findByIdentifier(identifier) ?: return runBlocking {
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
        var bytes: ByteArray? = null

        runBlocking {
            multiPartData.forEachPart { part ->
                if (part is PartData.FileItem) {
                    bytes = part.provider().readRemaining().readByteArray()
                }

                part.dispose()
            }
        }

        if (bytes == null) {
            return Response.badRequest("No file found")
        }

        try {
            val imageInputStream = ImageIO.createImageInputStream(ByteArrayInputStream(bytes))
            val imageReaders = ImageIO.getImageReaders(imageInputStream)
            require(imageReaders.hasNext()) { "Invalid file format" }
            val imageReader = imageReaders.next()
            val authorizedFormats = setOf("png", "jpeg", "jpg", "jpe")
            require(imageReader.formatName.lowercase() in authorizedFormats) { "Invalid file format, only png and jpeg are allowed. Received ${imageReader.formatName}" }
        } catch (e: Exception) {
            return Response.badRequest(e.message ?: "Invalid file format")
        }

        ImageService.add(
            memberUuid,
            ImageService.Type.IMAGE,
            bytes,
            128,
            128,
            true
        )

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
        return Response.ok(memberCacheService.getRefreshMember(memberUuid, limit))
    }
}