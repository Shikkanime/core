package fr.shikkanime.controllers.admin.api

import com.google.inject.Inject
import fr.shikkanime.controllers.admin.ADMIN
import fr.shikkanime.converters.AbstractConverter
import fr.shikkanime.dtos.RuleDto
import fr.shikkanime.entities.Rule
import fr.shikkanime.services.RuleService
import fr.shikkanime.utils.MapCache
import fr.shikkanime.utils.routes.AdminSessionAuthenticated
import fr.shikkanime.utils.routes.Controller
import fr.shikkanime.utils.routes.Path
import fr.shikkanime.utils.routes.Response
import fr.shikkanime.utils.routes.method.Delete
import fr.shikkanime.utils.routes.method.Get
import fr.shikkanime.utils.routes.method.Post
import fr.shikkanime.utils.routes.openapi.OpenAPI
import fr.shikkanime.utils.routes.param.BodyParam
import fr.shikkanime.utils.routes.param.PathParam
import java.util.*

@Controller("$ADMIN/api/rules")
class RuleController {
    @Inject
    private lateinit var ruleService: RuleService

    @Path
    @Get
    @AdminSessionAuthenticated
    @OpenAPI(hidden = true)
    private fun getRules(): Response {
        return Response.ok(AbstractConverter.convert(ruleService.findAll(), RuleDto::class.java))
    }

    @Path
    @Post
    @AdminSessionAuthenticated
    @OpenAPI(hidden = true)
    private fun createRule(@BodyParam ruleDto: RuleDto): Response {
        if (ruleDto.uuid != null) {
            return Response.badRequest("UUID must be null")
        }

        val rule = ruleService.save(AbstractConverter.convert(ruleDto, Rule::class.java))
        MapCache.invalidate(Rule::class.java)

        return Response.ok(AbstractConverter.convert(rule, RuleDto::class.java))
    }

    @Path("/{uuid}")
    @Delete
    @AdminSessionAuthenticated
    @OpenAPI(hidden = true)
    private fun deleteRule(@PathParam("uuid") uuid: UUID): Response {
        val rule = ruleService.find(uuid) ?: return Response.notFound()
        ruleService.delete(rule)
        MapCache.invalidate(Rule::class.java)
        return Response.noContent()
    }
}