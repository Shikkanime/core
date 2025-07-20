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
import fr.shikkanime.services.admin.AnimeAdminService
import fr.shikkanime.utils.MapCache
import fr.shikkanime.utils.routes.*
import fr.shikkanime.utils.routes.method.Delete
import fr.shikkanime.utils.routes.method.Get
import fr.shikkanime.utils.routes.method.Put
import fr.shikkanime.utils.routes.param.BodyParam
import fr.shikkanime.utils.routes.param.PathParam
import fr.shikkanime.utils.routes.param.QueryParam
import java.util.*

@Controller("$ADMIN/api/animes")
class AnimeController : HasPageableRoute() {
    @Inject private lateinit var animeService: AnimeService
    @Inject private lateinit var animeAdminService: AnimeAdminService
    @Inject private lateinit var animeFactory: AnimeFactory
    @Inject private lateinit var attachmentService: AttachmentService

    @Path("/force-update-all")
    @Get
    @AdminSessionAuthenticated
    private fun forceUpdateAllAnimes(): Response {
        animeAdminService.forceUpdateAll()
        MapCache.invalidate(Anime::class.java)
        return Response.redirect(Link.ANIMES.href)
    }

    @Path("/{uuid}/force-update")
    @Get
    @AdminSessionAuthenticated
    private fun forceUpdateAnime(@PathParam uuid: UUID): Response {
        animeAdminService.forceUpdate(uuid) ?: return Response.notFound()
        MapCache.invalidate(Anime::class.java)
        return Response.ok()
    }

    @Path("/{uuid}")
    @Get
    @AdminSessionAuthenticated
    private fun animeDetails(@PathParam uuid: UUID): Response {
        return Response.ok(animeFactory.toDto(animeService.find(uuid) ?: return Response.notFound()).apply {
            thumbnail = attachmentService.findByEntityUuidTypeAndActive(uuid, ImageType.THUMBNAIL)?.url
            banner = attachmentService.findByEntityUuidTypeAndActive(uuid, ImageType.BANNER)?.url
        })
    }

    @Path("/{uuid}")
    @Put
    @AdminSessionAuthenticated
    private fun updateAnime(
        @PathParam uuid: UUID,
        @BodyParam animeDto: AnimeDto
    ): Response {
        val updated = animeAdminService.update(uuid, animeDto) ?: return Response.notFound()
        MapCache.invalidate(Anime::class.java)
        return Response.ok(animeFactory.toDto(updated))
    }

    @Path("/{uuid}")
    @Delete
    @AdminSessionAuthenticated
    private fun deleteAnime(@PathParam uuid: UUID): Response {
        animeAdminService.delete(animeService.find(uuid) ?: return Response.notFound())
        MapCache.invalidate(Anime::class.java, EpisodeMapping::class.java, EpisodeVariant::class.java, Simulcast::class.java)
        return Response.noContent()
    }

    @Path("/alerts")
    @Get
    @AdminSessionAuthenticated
    private fun getAlerts(
        @QueryParam("page", "1") pageParam: Int,
        @QueryParam("limit", "9") limitParam: Int
    ): Response {
        val (page, limit, _) = pageableRoute(pageParam, limitParam, null, null)
        return Response.ok(animeAdminService.getAlerts(page, limit))
    }
}