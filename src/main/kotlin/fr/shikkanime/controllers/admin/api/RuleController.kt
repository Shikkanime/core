package fr.shikkanime.controllers.admin.api

import com.google.inject.Inject
import fr.shikkanime.controllers.admin.ADMIN
import fr.shikkanime.dtos.RuleDto
import fr.shikkanime.entities.Rule
import fr.shikkanime.factories.impl.RuleFactory
import fr.shikkanime.services.RuleService
import fr.shikkanime.utils.MapCache
import fr.shikkanime.utils.routes.AdminSessionAuthenticated
import fr.shikkanime.utils.routes.Controller
import fr.shikkanime.utils.routes.Path
import fr.shikkanime.utils.routes.Response
import fr.shikkanime.utils.routes.method.Delete
import fr.shikkanime.utils.routes.method.Get
import fr.shikkanime.utils.routes.method.Post
import fr.shikkanime.utils.routes.param.BodyParam
import fr.shikkanime.utils.routes.param.PathParam
import java.util.*

@Controller("$ADMIN/api/rules")
class RuleController {
    @Inject private lateinit var ruleService: RuleService
    @Inject private lateinit var ruleFactory: RuleFactory

    @Path
    @Get
    @AdminSessionAuthenticated
    private fun getRules(): Response {
        return Response.ok(ruleService.findAll().map { ruleFactory.toDto(it) })
    }

    @Path
    @Post
    @AdminSessionAuthenticated
    private fun createRule(@BodyParam ruleDto: RuleDto): Response {
        if (ruleDto.uuid != null) {
            return Response.badRequest("UUID must be null")
        }

        val rule = ruleService.save(ruleFactory.toEntity(ruleDto))
        MapCache.invalidate(Rule::class.java)
        return Response.ok(ruleFactory.toDto(rule))
    }

    @Path("/{uuid}")
    @Delete
    @AdminSessionAuthenticated
    private fun deleteRule(@PathParam("uuid") uuid: UUID): Response {
        val rule = ruleService.find(uuid) ?: return Response.notFound()
        ruleService.delete(rule)
        MapCache.invalidate(Rule::class.java)
        return Response.noContent()
    }
}