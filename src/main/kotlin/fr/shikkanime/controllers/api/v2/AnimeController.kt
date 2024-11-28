package fr.shikkanime.controllers.api.v2

import com.google.inject.Inject
import fr.shikkanime.dtos.*
import fr.shikkanime.dtos.weekly.v1.WeeklyAnimesDto
import fr.shikkanime.entities.enums.CountryCode
import fr.shikkanime.services.caches.AnimeCacheService
import fr.shikkanime.utils.atStartOfWeek
import fr.shikkanime.utils.routes.*
import fr.shikkanime.utils.routes.method.Get
import fr.shikkanime.utils.routes.openapi.OpenAPI
import fr.shikkanime.utils.routes.openapi.OpenAPIResponse
import fr.shikkanime.utils.routes.param.QueryParam
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.*

@Controller("/api/v2/animes")
class AnimeController : HasPageableRoute() {
    @Inject
    private lateinit var animeCacheService: AnimeCacheService

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
    ): Response {
        val startOfWeekDay = try {
            dateParam?.let { LocalDate.parse(it, DateTimeFormatter.ISO_LOCAL_DATE) } ?: LocalDate.now()
        } catch (_: Exception) {
            return Response.badRequest(MessageDto(MessageDto.Type.ERROR, "Invalid week format"))
        }.atStartOfWeek()

        return Response.ok(
            animeCacheService.getWeeklyAnimesV2(
                countryParam ?: CountryCode.FR,
                memberUuid,
                startOfWeekDay,
            )
        )
    }
}