package fr.shikkanime.controllers.api

import com.google.inject.Inject
import fr.shikkanime.entities.enums.CountryCode
import fr.shikkanime.services.caches.AnalyticsCacheService
import fr.shikkanime.utils.routes.Controller
import fr.shikkanime.utils.routes.Path
import fr.shikkanime.utils.routes.Response
import fr.shikkanime.utils.routes.method.Get
import fr.shikkanime.utils.routes.param.QueryParam
import java.time.LocalDate

@Controller("/api/v1/analytics")
class AnalyticsController {
    @Inject private lateinit var analyticsCacheService: AnalyticsCacheService

    @Path("/market-share")
    @Get
    fun getMarketShare(
        @QueryParam startYearParam: Int?,
        @QueryParam endYearParam: Int?
    ): Response {
        val now = LocalDate.now()
        val startYear = startYearParam ?: (now.year - 3)
        val endYear = endYearParam ?: now.year
        return Response.ok(analyticsCacheService.getAllMarketShare(startYear, endYear))
    }

    @Path("/sub-coverage")
    @Get
    fun getSubCoverage(
        @QueryParam(defaultValue = "FR") countryCode: CountryCode,
        @QueryParam startYearParam: Int?,
        @QueryParam endYearParam: Int?
    ): Response {
        val now = LocalDate.now()
        val startYear = startYearParam ?: (now.year - 3)
        val endYear = endYearParam ?: now.year
        return Response.ok(analyticsCacheService.getSubCoverage(countryCode, startYear, endYear))
    }

    @Path("/genre-coverage")
    @Get
    fun getGenreCoverage(
        @QueryParam startYearParam: Int?,
        @QueryParam endYearParam: Int?
    ): Response {
        val now = LocalDate.now()
        val startYear = startYearParam ?: (now.year - 3)
        val endYear = endYearParam ?: now.year
        return Response.ok(analyticsCacheService.getAllGenreCoverage(startYear, endYear))
    }
}