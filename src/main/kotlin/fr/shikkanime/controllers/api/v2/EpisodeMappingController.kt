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
        @QueryParam("limit", "9") limitParam: Int,
        @QueryParam("sort") sortParam: String?,
        @QueryParam("desc") descParam: String?
    ): Response {
        val (page, limit, sortParameters) = if (sortParam.isNullOrBlank() && descParam.isNullOrBlank()) {
            pageableRoute(pageParam, limitParam, "releaseDateTime,animeName,season,episodeType,number", "releaseDateTime,animeName,season,episodeType,number")
        } else {
            pageableRoute(pageParam, limitParam, sortParam, descParam)
        }

        return Response.ok(episodeMappingCacheService.findAllGroupedBy(country, sortParameters, page, limit))
    }
}