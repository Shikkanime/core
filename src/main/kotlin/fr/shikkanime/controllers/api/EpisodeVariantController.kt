package fr.shikkanime.controllers.api

import com.google.inject.Inject
import fr.shikkanime.dtos.variants.SeparateVariantDto
import fr.shikkanime.services.EpisodeVariantService
import fr.shikkanime.utils.routes.*
import fr.shikkanime.utils.routes.method.Post
import fr.shikkanime.utils.routes.openapi.OpenAPI
import fr.shikkanime.utils.routes.param.BodyParam
import fr.shikkanime.utils.routes.param.PathParam
import java.util.*

@Controller("/api/v1/episode-variants")
class EpisodeVariantController : HasPageableRoute() {
    @Inject
    private lateinit var episodeVariantService: EpisodeVariantService

    @Path("/{uuid}/separate")
    @Post
    @AdminSessionAuthenticated
    @OpenAPI(hidden = true)
    private fun separateVariant(
        @PathParam("uuid") uuid: UUID,
        @BodyParam separateVariantDto: SeparateVariantDto
    ): Response {
        episodeVariantService.separate(uuid, separateVariantDto)
        return Response.created()
    }
}