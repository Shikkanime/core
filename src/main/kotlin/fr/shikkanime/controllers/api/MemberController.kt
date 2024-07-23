package fr.shikkanime.controllers.api

import com.google.inject.Inject
import fr.shikkanime.dtos.AllFollowedEpisodeDto
import fr.shikkanime.dtos.GenericDto
import fr.shikkanime.services.ImageService
import fr.shikkanime.services.MemberFollowAnimeService
import fr.shikkanime.services.MemberFollowEpisodeService
import fr.shikkanime.services.MemberService
import fr.shikkanime.services.caches.MemberCacheService
import fr.shikkanime.utils.StringUtils
import fr.shikkanime.utils.routes.*
import fr.shikkanime.utils.routes.method.Delete
import fr.shikkanime.utils.routes.method.Post
import fr.shikkanime.utils.routes.method.Put
import fr.shikkanime.utils.routes.openapi.OpenAPI
import fr.shikkanime.utils.routes.openapi.OpenAPIResponse
import fr.shikkanime.utils.routes.param.BodyParam
import io.ktor.http.content.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import java.io.ByteArrayInputStream
import java.util.*
import javax.imageio.ImageIO

@Controller("/api/v1/members")
class MemberController {
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
    private fun associateEmail(@JWTUser uuidUser: UUID, @BodyParam email: String): Response {
        // Verify email
        if (!StringUtils.isValidEmail(email)) {
            return Response.badRequest("Invalid email")
        }

        if (memberService.findByEmail(email) != null) {
            return Response.conflict("Email already associated to an account")
        }

        return Response.created(GenericDto(memberService.associateEmail(uuidUser, email)))
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
    private fun forgotIdentifier(@JWTUser uuidUser: UUID, @BodyParam email: String): Response {
        // Verify email
        if (!StringUtils.isValidEmail(email)) {
            return Response.badRequest("Invalid email")
        }

        if (memberCacheService.find(uuidUser)!!.email == email) {
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
    private fun followAnime(@JWTUser uuidUser: UUID, @BodyParam anime: GenericDto): Response {
        return memberFollowAnimeService.follow(uuidUser, anime)
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
    private fun unfollowAnime(@JWTUser uuidUser: UUID, @BodyParam anime: GenericDto): Response {
        return memberFollowAnimeService.unfollow(uuidUser, anime)
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
    private fun followAllEpisodes(@JWTUser uuidUser: UUID, @BodyParam anime: GenericDto): Response {
        return memberFollowEpisodeService.followAll(uuidUser, anime)
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
    private fun followEpisode(@JWTUser uuidUser: UUID, @BodyParam episode: GenericDto): Response {
        return memberFollowEpisodeService.follow(uuidUser, episode)
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
    private fun unfollowEpisode(@JWTUser uuidUser: UUID, @BodyParam episode: GenericDto): Response {
        return memberFollowEpisodeService.unfollow(uuidUser, episode)
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
    private fun uploadProfileImage(@JWTUser uuidUser: UUID, @BodyParam multiPartData: MultiPartData): Response {
        val file = runBlocking { multiPartData.readAllParts().filterIsInstance<PartData.FileItem>().firstOrNull() }
            ?: return Response.badRequest("No file provided")
        val bytes = file.streamProvider().readBytes()

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
            uuidUser,
            ImageService.Type.IMAGE,
            bytes,
            128,
            128,
            true
        )

        return Response.ok()
    }
}