package fr.shikkanime.controllers.api.v2

import com.google.inject.Inject
import fr.shikkanime.entities.enums.CountryCode
import fr.shikkanime.services.caches.EpisodeMappingCacheService
import fr.shikkanime.utils.routes.Controller
import fr.shikkanime.utils.routes.HasPageableRoute
import fr.shikkanime.utils.routes.Path
import fr.shikkanime.utils.routes.Response
import fr.shikkanime.utils.routes.method.Get
import fr.shikkanime.utils.routes.param.QueryParam

@Controller("/api/v2/episode-mappings")
class EpisodeMappingController : HasPageableRoute() {
    @Inject private lateinit var episodeMappingCacheService: EpisodeMappingCacheService

    @Path
    @Get
    private fun getAll(
        @QueryParam country: CountryCode?,
        @QueryParam("page", "1") pageParam: Int,
        @QueryParam("limit", "9") limitParam: Int
    ): Response {
        val (page, limit, _) = pageableRoute(pageParam, limitParam, null, null)
        return Response.ok(episodeMappingCacheService.findAllGroupedBy(country, page, limit))
    }
}