package fr.shikkanime.controllers.api.v2

import com.google.inject.Inject
import fr.shikkanime.dtos.PageableDto
import fr.shikkanime.entities.enums.CountryCode
import fr.shikkanime.services.caches.EpisodeMappingCacheService
import fr.shikkanime.utils.routes.Controller
import fr.shikkanime.utils.routes.HasPageableRoute
import fr.shikkanime.utils.routes.Path
import fr.shikkanime.utils.routes.Response
import fr.shikkanime.utils.routes.method.Get
import fr.shikkanime.utils.routes.openapi.OpenAPI
import fr.shikkanime.utils.routes.openapi.OpenAPIResponse
import fr.shikkanime.utils.routes.param.QueryParam

@Controller("/api/v2/episode-mappings")
class EpisodeMappingController : HasPageableRoute() {
    @Inject
    private lateinit var episodeMappingCacheService: EpisodeMappingCacheService

    @Path
    @Get
    @OpenAPI(
        "Get episode mappings",
        [
            OpenAPIResponse(
                200,
                "Episode mappings found",
                PageableDto::class,
            )
        ]
    )
    private fun getAll(
        @QueryParam("country", description = "Country code to filter by", example = "FR", type = CountryCode::class)
        countryParam: CountryCode?,
        @QueryParam("page", description = "Page number for pagination")
        pageParam: Int?,
        @QueryParam("limit", description = "Number of items per page. Must be between 1 and 30", example = "15")
        limitParam: Int?,
    ): Response {
        val (page, limit, _) = pageableRoute(pageParam, limitParam, null, null)

        return Response.ok(
            episodeMappingCacheService.findAllGroupedBy(
                countryParam ?: CountryCode.FR,
                page,
                limit,
            )
        )
    }
}