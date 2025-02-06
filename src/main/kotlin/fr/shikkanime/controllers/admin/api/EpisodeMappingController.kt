package fr.shikkanime.controllers.admin.api

import com.google.inject.Inject
import fr.shikkanime.controllers.admin.ADMIN
import fr.shikkanime.converters.AbstractConverter
import fr.shikkanime.dtos.mappings.EpisodeMappingDto
import fr.shikkanime.dtos.mappings.UpdateAllEpisodeMappingDto
import fr.shikkanime.entities.Anime
import fr.shikkanime.entities.EpisodeMapping
import fr.shikkanime.entities.EpisodeVariant
import fr.shikkanime.entities.Simulcast
import fr.shikkanime.services.EpisodeMappingService
import fr.shikkanime.utils.MapCache
import fr.shikkanime.utils.routes.AdminSessionAuthenticated
import fr.shikkanime.utils.routes.Controller
import fr.shikkanime.utils.routes.Path
import fr.shikkanime.utils.routes.Response
import fr.shikkanime.utils.routes.method.Delete
import fr.shikkanime.utils.routes.method.Get
import fr.shikkanime.utils.routes.method.Put
import fr.shikkanime.utils.routes.openapi.OpenAPI
import fr.shikkanime.utils.routes.param.BodyParam
import fr.shikkanime.utils.routes.param.PathParam
import java.util.*

@Controller("$ADMIN/api/episode-mappings")
class EpisodeMappingController {
    @Inject
    private lateinit var episodeMappingService: EpisodeMappingService

    @Path("/update-all")
    @Put
    @AdminSessionAuthenticated
    @OpenAPI(hidden = true)
    private fun updateAllEpisode(
        @BodyParam updateAllEpisodeMappingDto: UpdateAllEpisodeMappingDto
    ): Response {
        if (updateAllEpisodeMappingDto.uuids.isEmpty()) {
            return Response.badRequest("uuids must not be empty")
        }

        if (updateAllEpisodeMappingDto.episodeType == null &&
            updateAllEpisodeMappingDto.season == null &&
            updateAllEpisodeMappingDto.startDate == null &&
            updateAllEpisodeMappingDto.incrementDate == null &&
            updateAllEpisodeMappingDto.forceUpdate == null) {
            return Response.badRequest("At least one field must be set")
        }

        episodeMappingService.updateAll(updateAllEpisodeMappingDto)
        MapCache.invalidate(Anime::class.java, Simulcast::class.java, EpisodeMapping::class.java, EpisodeVariant::class.java)
        return Response.ok()
    }

    @Path("/{uuid}")
    @Get
    @AdminSessionAuthenticated
    @OpenAPI(hidden = true)
    private fun read(@PathParam("uuid") uuid: UUID): Response {
        val find = episodeMappingService.find(uuid) ?: return Response.notFound()
        return Response.ok(AbstractConverter.convert(find, EpisodeMappingDto::class.java))
    }

    @Path("/{uuid}")
    @Put
    @AdminSessionAuthenticated
    @OpenAPI(hidden = true)
    private fun updateEpisode(
        @PathParam("uuid") uuid: UUID,
        @BodyParam episodeMappingDto: EpisodeMappingDto
    ): Response {
        val updated = episodeMappingService.update(uuid, episodeMappingDto)
        MapCache.invalidate(Anime::class.java, EpisodeMapping::class.java, EpisodeVariant::class.java, Simulcast::class.java)
        return Response.ok(AbstractConverter.convert(updated, EpisodeMappingDto::class.java))
    }

    @Path("/{uuid}")
    @Delete
    @AdminSessionAuthenticated
    @OpenAPI(hidden = true)
    private fun deleteEpisode(@PathParam("uuid") uuid: UUID): Response {
        episodeMappingService.delete(episodeMappingService.find(uuid) ?: return Response.notFound())
        MapCache.invalidate(EpisodeMapping::class.java, EpisodeVariant::class.java)
        return Response.noContent()
    }
}