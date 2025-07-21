package fr.shikkanime.controllers.api.v2

import com.google.inject.Inject
import fr.shikkanime.entities.enums.CountryCode
import fr.shikkanime.services.caches.EpisodeMappingCacheService
import fr.shikkanime.services.caches.MemberFollowEpisodeCacheService
import fr.shikkanime.utils.routes.*
import fr.shikkanime.utils.routes.method.Get
import fr.shikkanime.utils.routes.param.QueryParam
import java.util.*

@Controller("/api/v2/episode-mappings")
class EpisodeMappingController : HasPageableRoute() {
    @Inject private lateinit var episodeMappingCacheService: EpisodeMappingCacheService
    @Inject private lateinit var memberFollowEpisodeCacheService: MemberFollowEpisodeCacheService

    @Path
    @Get
    @JWTAuthenticated(optional = true)
    private fun getAll(
        @JWTUser memberUuid: UUID?,
        @QueryParam country: CountryCode?,
        @QueryParam("page", "1") pageParam: Int,
        @QueryParam("limit", "9") limitParam: Int
    ): Response {
        val (page, limit, _) = pageableRoute(pageParam, limitParam, null, null)
        val episodes = episodeMappingCacheService.findAllGroupedBy(country, page, limit)

        if (memberUuid != null) {
            episodes.data.asSequence()
                .filter { grouped -> grouped.mappings.size == 1 }
                .forEach { grouped -> grouped.inWatchlist = memberFollowEpisodeCacheService.existsByMemberAndEpisode(memberUuid, grouped.mappings.first()) }
        }

        return Response.ok(episodes)
    }
}