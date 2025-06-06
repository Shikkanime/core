package fr.shikkanime.controllers.admin.api

import com.google.inject.Inject
import fr.shikkanime.controllers.admin.ADMIN
import fr.shikkanime.dtos.MessageDto
import fr.shikkanime.dtos.mappings.EpisodeMappingDto
import fr.shikkanime.dtos.mappings.UpdateAllEpisodeMappingDto
import fr.shikkanime.entities.Anime
import fr.shikkanime.entities.EpisodeMapping
import fr.shikkanime.entities.EpisodeVariant
import fr.shikkanime.entities.Simulcast
import fr.shikkanime.entities.enums.ImageType
import fr.shikkanime.factories.impl.EpisodeMappingFactory
import fr.shikkanime.services.AttachmentService
import fr.shikkanime.services.EpisodeMappingService
import fr.shikkanime.services.admin.EpisodeMappingAdminService
import fr.shikkanime.utils.MapCache
import fr.shikkanime.utils.routes.AdminSessionAuthenticated
import fr.shikkanime.utils.routes.Controller
import fr.shikkanime.utils.routes.Path
import fr.shikkanime.utils.routes.Response
import fr.shikkanime.utils.routes.method.Delete
import fr.shikkanime.utils.routes.method.Get
import fr.shikkanime.utils.routes.method.Put
import fr.shikkanime.utils.routes.param.BodyParam
import fr.shikkanime.utils.routes.param.PathParam
import java.util.*

@Controller("$ADMIN/api/episode-mappings")
class EpisodeMappingController {
    @Inject private lateinit var episodeMappingService: EpisodeMappingService
    @Inject private lateinit var episodeMappingAdminService: EpisodeMappingAdminService
    @Inject private lateinit var episodeMappingFactory: EpisodeMappingFactory
    @Inject private lateinit var attachmentService: AttachmentService

    @Path("/update-all")
    @Put
    @AdminSessionAuthenticated
    private fun updateAllEpisode(@BodyParam updateAllEpisodeMappingDto: UpdateAllEpisodeMappingDto): Response {
        if (updateAllEpisodeMappingDto.uuids.isEmpty())
            return Response.badRequest(MessageDto.error("uuids must not be empty"))

        if (updateAllEpisodeMappingDto.animeName == null &&
            updateAllEpisodeMappingDto.season == null &&
            updateAllEpisodeMappingDto.episodeType == null &&
            updateAllEpisodeMappingDto.startDate == null &&
            updateAllEpisodeMappingDto.incrementDate == null &&
            updateAllEpisodeMappingDto.forceUpdate == null &&
            updateAllEpisodeMappingDto.bindVoiceVariants == null &&
            updateAllEpisodeMappingDto.bindNumber == null)
            return Response.badRequest(MessageDto.error("At least one field must be set"))

        episodeMappingAdminService.updateAll(updateAllEpisodeMappingDto)
        MapCache.invalidate(Anime::class.java, Simulcast::class.java, EpisodeMapping::class.java, EpisodeVariant::class.java)
        return Response.ok()
    }

    @Path("/{uuid}")
    @Get
    @AdminSessionAuthenticated
    private fun read(@PathParam uuid: UUID): Response {
        val episodeMapping = episodeMappingService.find(uuid) ?: return Response.notFound()
        return Response.ok(episodeMappingFactory.toDto(episodeMapping).apply { image = attachmentService.findByEntityUuidTypeAndActive(uuid, ImageType.BANNER)?.url })
    }

    @Path("/{uuid}")
    @Put
    @AdminSessionAuthenticated
    private fun updateEpisode(
        @PathParam uuid: UUID,
        @BodyParam episodeMappingDto: EpisodeMappingDto
    ): Response {
        val updated = episodeMappingAdminService.update(uuid, episodeMappingDto)
        MapCache.invalidate(Anime::class.java, EpisodeMapping::class.java, EpisodeVariant::class.java, Simulcast::class.java)
        return Response.ok(episodeMappingFactory.toDto(updated ?: return Response.noContent()))
    }

    @Path("/{uuid}")
    @Delete
    @AdminSessionAuthenticated
    private fun deleteEpisode(@PathParam uuid: UUID): Response {
        episodeMappingService.delete(episodeMappingService.find(uuid) ?: return Response.notFound())
        MapCache.invalidate(EpisodeMapping::class.java, EpisodeVariant::class.java)
        return Response.noContent()
    }
}