package fr.shikkanime.controllers.api

import com.google.inject.Inject
import fr.shikkanime.dtos.AllFollowedEpisodeDto
import fr.shikkanime.dtos.GenericDto
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
import java.util.*

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

    @Path("/private-register")
    @Post
    @OpenAPI(
        description = "Register a private member",
        responses = [
            OpenAPIResponse(201, "Private member registered", Map::class),
        ]
    )
    private fun registerPrivateMember(): Response {
        var identifier: String

        do {
            identifier = StringUtils.generateRandomString(12)
        } while (memberCacheService.findPrivateMember(identifier) != null)

        memberService.savePrivateMember(identifier)
        return Response.created(mapOf("identifier" to identifier))
    }

    @Path("/private-login")
    @Post
    @OpenAPI(
        description = "Login a private member",
        responses = [
            OpenAPIResponse(200, "Private member logged in"),
        ]
    )
    private fun loginPrivateMember(@BodyParam identifier: String): Response {
        return Response.ok(memberCacheService.findPrivateMember(identifier) ?: return Response.notFound())
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
}