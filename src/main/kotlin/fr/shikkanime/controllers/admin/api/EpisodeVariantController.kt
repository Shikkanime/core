package fr.shikkanime.controllers.admin.api

import com.google.inject.Inject
import fr.shikkanime.controllers.admin.ADMIN
import fr.shikkanime.dtos.variants.SeparateVariantDto
import fr.shikkanime.entities.EpisodeMapping
import fr.shikkanime.entities.EpisodeVariant
import fr.shikkanime.services.EpisodeVariantService
import fr.shikkanime.utils.MapCache
import fr.shikkanime.utils.routes.*
import fr.shikkanime.utils.routes.method.Post
import fr.shikkanime.utils.routes.param.BodyParam
import fr.shikkanime.utils.routes.param.PathParam
import java.util.*

@Controller("$ADMIN/api/episode-variants")
class EpisodeVariantController : HasPageableRoute() {
    @Inject
    private lateinit var episodeVariantService: EpisodeVariantService

    @Path("/{uuid}/separate")
    @Post
    @AdminSessionAuthenticated
    private fun separateVariant(
        @PathParam uuid: UUID,
        @BodyParam separateVariantDto: SeparateVariantDto
    ): Response {
        episodeVariantService.separate(uuid, separateVariantDto)
        MapCache.invalidate(EpisodeMapping::class.java, EpisodeVariant::class.java)
        return Response.created()
    }
}