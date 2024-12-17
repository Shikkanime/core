package fr.shikkanime.controllers.api

import com.google.inject.Inject
import fr.shikkanime.dtos.PageableDto
import fr.shikkanime.dtos.TraceActionDto
import fr.shikkanime.services.TraceActionService
import fr.shikkanime.utils.routes.*
import fr.shikkanime.utils.routes.method.Get
import fr.shikkanime.utils.routes.openapi.OpenAPI
import fr.shikkanime.utils.routes.param.QueryParam

@Controller("/api/trace-actions")
class TraceActionController : HasPageableRoute() {
    @Inject
    private lateinit var traceActionService: TraceActionService

    @Path
    @Get
    @AdminSessionAuthenticated
    @OpenAPI(hidden = true)
    private fun getTraceAction(
        @QueryParam("entityType", description = "Entity type to filter by")
        entityTypeParam: String?,
        @QueryParam("action", description = "Action to filter by")
        actionParam: String?,
        @QueryParam("page", description = "Page number for pagination")
        pageParam: Int?,
        @QueryParam("limit", description = "Number of items per page. Must be between 1 and 30", example = "15")
        limitParam: Int?,
    ): Response {
        val (page, limit, _) = pageableRoute(pageParam, limitParam, null, null)
        return Response.ok(PageableDto.fromPageable(traceActionService.findAllBy(entityTypeParam, actionParam, page, limit), TraceActionDto::class.java))
    }

    @Path("/login-counts")
    @Get
    @AdminSessionAuthenticated
    @OpenAPI(hidden = true)
    private fun getLoginCounts(): Response {
        return Response.ok(traceActionService.getLoginCounts())
    }
}