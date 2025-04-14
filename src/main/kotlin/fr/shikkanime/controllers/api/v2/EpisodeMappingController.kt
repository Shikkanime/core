package fr.shikkanime.controllers.api.v2

import com.google.inject.Inject
import fr.shikkanime.entities.enums.CountryCode
import fr.shikkanime.services.caches.GroupedEpisodeCacheService
import fr.shikkanime.utils.routes.Controller
import fr.shikkanime.utils.routes.HasPageableRoute
import fr.shikkanime.utils.routes.Path
import fr.shikkanime.utils.routes.Response
import fr.shikkanime.utils.routes.method.Get
import fr.shikkanime.utils.routes.param.QueryParam

@Controller("/api/v2/episode-mappings")
class EpisodeMappingController : HasPageableRoute() {
    @Inject
    private lateinit var groupedEpisodeCacheService: GroupedEpisodeCacheService

    @Path
    @Get
    private fun getAll(
        @QueryParam("country")
        countryParam: CountryCode?,
        @QueryParam("page")
        pageParam: Int?,
        @QueryParam("limit")
        limitParam: Int?,
    ): Response {
        val (page, limit, _) = pageableRoute(pageParam, limitParam, null, null)

        return Response.ok(
            groupedEpisodeCacheService.findAllBy(
                countryParam ?: CountryCode.FR,
                page,
                limit,
            )
        )
    }
}