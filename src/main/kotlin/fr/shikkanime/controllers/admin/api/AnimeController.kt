package fr.shikkanime.controllers.admin.api

import com.google.inject.Inject
import fr.shikkanime.controllers.admin.ADMIN
import fr.shikkanime.converters.AbstractConverter
import fr.shikkanime.dtos.animes.AnimeDto
import fr.shikkanime.entities.Anime
import fr.shikkanime.entities.EpisodeMapping
import fr.shikkanime.entities.EpisodeVariant
import fr.shikkanime.entities.Simulcast
import fr.shikkanime.entities.enums.Link
import fr.shikkanime.services.AnimeService
import fr.shikkanime.utils.MapCache
import fr.shikkanime.utils.routes.*
import fr.shikkanime.utils.routes.method.Delete
import fr.shikkanime.utils.routes.method.Get
import fr.shikkanime.utils.routes.method.Put
import fr.shikkanime.utils.routes.openapi.OpenAPI
import fr.shikkanime.utils.routes.param.BodyParam
import fr.shikkanime.utils.routes.param.PathParam
import fr.shikkanime.utils.routes.param.QueryParam
import java.util.*

@Controller("$ADMIN/api/animes")
class AnimeController : HasPageableRoute() {
    @Inject
    private lateinit var animeService: AnimeService

    @Path("/force-update-all")
    @Get
    @AdminSessionAuthenticated
    private fun forceUpdateAllAnimes(): Response {
        val animes = animeService.findAll()
        animes.forEach { it.lastUpdateDateTime = null }
        animeService.updateAll(animes)

        return Response.redirect(Link.ANIMES.href)
    }

    @Path("/{uuid}")
    @Get
    @AdminSessionAuthenticated
    @OpenAPI(hidden = true)
    private fun animeDetails(
        @PathParam("uuid") uuid: UUID,
    ): Response {
        return Response.ok(AbstractConverter.convert(animeService.find(uuid) ?: return Response.notFound(), AnimeDto::class.java))
    }

    @Path("/{uuid}")
    @Put
    @AdminSessionAuthenticated
    @OpenAPI(hidden = true)
    private fun updateAnime(@PathParam("uuid") uuid: UUID, @BodyParam animeDto: AnimeDto): Response {
        val updated = animeService.update(uuid, animeDto)
        MapCache.invalidate(Anime::class.java)
        return Response.ok(AbstractConverter.convert(updated, AnimeDto::class.java))
    }

    @Path("/{uuid}")
    @Delete
    @AdminSessionAuthenticated
    @OpenAPI(hidden = true)
    private fun deleteAnime(@PathParam("uuid") uuid: UUID): Response {
        animeService.delete(animeService.find(uuid) ?: return Response.notFound())
        MapCache.invalidate(Anime::class.java, EpisodeMapping::class.java, EpisodeVariant::class.java, Simulcast::class.java)
        return Response.noContent()
    }

    @Path("/alerts")
    @Get
    @AdminSessionAuthenticated
    @OpenAPI(hidden = true)
    private fun getAlerts(
        @QueryParam("page", description = "Page number for pagination")
        pageParam: Int?,
        @QueryParam("limit", description = "Number of items per page. Must be between 1 and 30", example = "15")
        limitParam: Int?,
    ): Response {
        val (page, limit, _) = pageableRoute(pageParam, limitParam, null, null)
        return Response.ok(animeService.getAlerts(page, limit))
    }
}