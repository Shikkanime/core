package fr.shikkanime.controllers.site

import com.google.inject.Inject
import fr.shikkanime.dtos.variants.EpisodeVariantDto
import fr.shikkanime.entities.Member
import fr.shikkanime.services.MemberActionService
import fr.shikkanime.services.caches.EpisodeVariantCacheService
import fr.shikkanime.utils.Constant
import fr.shikkanime.utils.MapCache
import fr.shikkanime.utils.routes.Controller
import fr.shikkanime.utils.routes.Path
import fr.shikkanime.utils.routes.Response
import fr.shikkanime.utils.routes.method.Get
import fr.shikkanime.utils.routes.param.PathParam
import java.util.*

@Controller("/")
class ShortLinkController {
    @Inject private lateinit var memberActionService: MemberActionService
    @Inject private lateinit var episodeVariantCacheService: EpisodeVariantCacheService

    private fun getUrl(episodeDto: EpisodeVariantDto) =
        "${Constant.baseUrl}/animes/${episodeDto.mapping!!.anime!!.slug}/season-${episodeDto.mapping.season}/${episodeDto.mapping.episodeType.slug}-${episodeDto.mapping.number}"

    @Path("r/{episodeVariantUuid}")
    @Get
    private fun redirectToRealLink(@PathParam episodeVariantUuid: UUID) =
        Response.redirect(episodeVariantCacheService.find(episodeVariantUuid)?.let { getUrl(it) } ?: "/404")

    @Path("v/{webToken}")
    @Get
    private fun validateWebToken(@PathParam webToken: String): Response {
        try {
            memberActionService.validateWebAction(webToken)
            MapCache.invalidate(Member::class.java)
            return Response.template("/site/validateAction.ftl")
        } catch (_: Exception) {
            return Response.redirect("/")
        }
    }
}