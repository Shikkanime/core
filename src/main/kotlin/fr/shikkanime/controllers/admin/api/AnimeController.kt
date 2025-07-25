package fr.shikkanime.controllers.admin.api

import com.google.inject.Inject
import fr.shikkanime.controllers.admin.ADMIN
import fr.shikkanime.dtos.animes.AnimeDto
import fr.shikkanime.entities.Anime
import fr.shikkanime.entities.EpisodeMapping
import fr.shikkanime.entities.EpisodeVariant
import fr.shikkanime.entities.Simulcast
import fr.shikkanime.entities.enums.ImageType
import fr.shikkanime.entities.enums.Link
import fr.shikkanime.factories.impl.AnimeFactory
import fr.shikkanime.services.AnimeService
import fr.shikkanime.services.AttachmentService
import fr.shikkanime.services.EpisodeVariantService
import fr.shikkanime.services.admin.AnimeAdminService
import fr.shikkanime.utils.InvalidationService
import fr.shikkanime.utils.routes.*
import fr.shikkanime.utils.routes.method.Delete
import fr.shikkanime.utils.routes.method.Get
import fr.shikkanime.utils.routes.method.Put
import fr.shikkanime.utils.routes.param.BodyParam
import fr.shikkanime.utils.routes.param.PathParam
import java.util.*

@Controller("$ADMIN/api/animes")
@AdminSessionAuthenticated
class AnimeController : HasPageableRoute() {
    @Inject private lateinit var animeService: AnimeService
    @Inject private lateinit var animeAdminService: AnimeAdminService
    @Inject private lateinit var animeFactory: AnimeFactory
    @Inject private lateinit var attachmentService: AttachmentService
    @Inject private lateinit var episodeVariantService: EpisodeVariantService

    @Path("/force-update-all")
    @Get
    private fun forceUpdateAllAnimes(): Response {
        animeAdminService.forceUpdateAll()
        InvalidationService.invalidate(Anime::class.java)
        return Response.redirect(Link.ANIMES.href)
    }

    @Path("/{uuid}/force-update")
    @Get
    private fun forceUpdateAnime(@PathParam uuid: UUID): Response {
        animeAdminService.forceUpdate(uuid) ?: return Response.notFound()
        InvalidationService.invalidate(Anime::class.java)
        return Response.ok()
    }

    @Path("/{uuid}")
    @Get
    private fun animeDetails(@PathParam uuid: UUID): Response {
        return Response.ok(animeFactory.toDto(animeService.find(uuid) ?: return Response.notFound(), true).apply {
            thumbnail = attachmentService.findByEntityUuidTypeAndActive(uuid, ImageType.THUMBNAIL)?.url
            banner = attachmentService.findByEntityUuidTypeAndActive(uuid, ImageType.BANNER)?.url
            carousel = attachmentService.findByEntityUuidTypeAndActive(uuid, ImageType.CAROUSEL)?.url
        })
    }

    @Path("/{uuid}")
    @Put
    private fun updateAnime(
        @PathParam uuid: UUID,
        @BodyParam animeDto: AnimeDto
    ): Response {
        val updated = animeAdminService.update(uuid, animeDto) ?: return Response.notFound()
        episodeVariantService.preIndex()
        InvalidationService.invalidate(Anime::class.java)
        return Response.ok(animeFactory.toDto(updated))
    }

    @Path("/{uuid}")
    @Delete
    private fun deleteAnime(@PathParam uuid: UUID): Response {
        animeAdminService.delete(animeService.find(uuid) ?: return Response.notFound())
        episodeVariantService.preIndex()
        InvalidationService.invalidate(Anime::class.java, EpisodeMapping::class.java, EpisodeVariant::class.java, Simulcast::class.java)
        return Response.noContent()
    }
}