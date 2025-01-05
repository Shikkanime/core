package fr.shikkanime.controllers.api.v2

import com.google.inject.Inject
import fr.shikkanime.controllers.api.AnimeController
import fr.shikkanime.dtos.*
import fr.shikkanime.dtos.weekly.WeeklyAnimesDto
import fr.shikkanime.entities.enums.CountryCode
import fr.shikkanime.utils.routes.*
import fr.shikkanime.utils.routes.method.Get
import fr.shikkanime.utils.routes.openapi.OpenAPI
import fr.shikkanime.utils.routes.openapi.OpenAPIResponse
import fr.shikkanime.utils.routes.param.QueryParam
import java.util.*

@Controller("/api/v2/animes")
class AnimeController : HasPageableRoute() {
    @Inject
    private lateinit var animeController: AnimeController

    @Deprecated("Use /api/v1/animes/weekly instead")
    @Path("/weekly")
    @Get
    @JWTAuthenticated(optional = true)
    @OpenAPI(
        "Get weekly anime",
        [
            OpenAPIResponse(200, "Weekly anime found", Array<WeeklyAnimesDto>::class),
            OpenAPIResponse(400, "Invalid week format", MessageDto::class),
            OpenAPIResponse(401, "Unauthorized")
        ],
        security = true
    )
    private fun getWeekly(
        @JWTUser
        memberUuid: UUID?,
        @QueryParam("country", description = "Country code to filter by", example = "FR", type = CountryCode::class)
        countryParam: CountryCode?,
        @QueryParam(
            "date",
            description = "Date to filter by. Format: yyyy-MM-dd",
            example = "2021-01-01"
        )
        dateParam: String?,
    ) = animeController.getWeekly(memberUuid, countryParam, dateParam)
}