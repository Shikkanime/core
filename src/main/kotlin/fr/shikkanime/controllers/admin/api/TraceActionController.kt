package fr.shikkanime.controllers.admin.api

import com.google.inject.Inject
import fr.shikkanime.controllers.admin.ADMIN
import fr.shikkanime.dtos.PageableDto
import fr.shikkanime.factories.impl.TraceActionFactory
import fr.shikkanime.services.TraceActionService
import fr.shikkanime.utils.routes.*
import fr.shikkanime.utils.routes.method.Get
import fr.shikkanime.utils.routes.param.QueryParam
import java.time.ZonedDateTime

@Controller("$ADMIN/api/trace-actions")
class TraceActionController : HasPageableRoute() {
    @Inject
    private lateinit var traceActionService: TraceActionService

    @Inject
    private lateinit var traceActionFactory: TraceActionFactory

    @Path
    @Get
    @AdminSessionAuthenticated
    private fun getTraceAction(
        @QueryParam("entityType",)
        entityTypeParam: String?,
        @QueryParam("action")
        actionParam: String?,
        @QueryParam("page")
        pageParam: Int?,
        @QueryParam("limit")
        limitParam: Int?,
    ): Response {
        val (page, limit, _) = pageableRoute(pageParam, limitParam, null, null)
        return Response.ok(PageableDto.fromPageable(traceActionService.findAllBy(entityTypeParam, actionParam, page, limit), traceActionFactory))
    }

    @Path("/login-counts")
    @Get
    @AdminSessionAuthenticated
    private fun getLoginCounts(
        @QueryParam("days") days: Int?,
    ): Response {
        val xDayAgo = ZonedDateTime.now().minusDays(days?.toLong() ?: 30)
        return Response.ok(traceActionService.getLoginCountsAfter(xDayAgo))
    }
}