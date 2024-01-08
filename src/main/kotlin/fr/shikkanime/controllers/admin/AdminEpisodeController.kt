package fr.shikkanime.controllers.admin

import com.google.inject.Inject
import fr.shikkanime.converters.AbstractConverter
import fr.shikkanime.dtos.EpisodeDto
import fr.shikkanime.entities.enums.EpisodeType
import fr.shikkanime.entities.enums.LangType
import fr.shikkanime.entities.enums.Link
import fr.shikkanime.services.EpisodeService
import fr.shikkanime.utils.routes.AdminSessionAuthenticated
import fr.shikkanime.utils.routes.Controller
import fr.shikkanime.utils.routes.Path
import fr.shikkanime.utils.routes.Response
import fr.shikkanime.utils.routes.method.Get
import fr.shikkanime.utils.routes.method.Post
import fr.shikkanime.utils.routes.param.BodyParam
import fr.shikkanime.utils.routes.param.PathParam
import io.ktor.http.*
import java.util.*

@Controller("/admin/episodes")
class AdminEpisodeController {
    @Inject
    private lateinit var episodeService: EpisodeService

    @Path
    @Get
    @AdminSessionAuthenticated
    private fun getEpisodes(): Response {
        return Response.template(Link.EPISODES)
    }

    @Path("/{uuid}")
    @Get
    @AdminSessionAuthenticated
    private fun getEpisodeView(@PathParam("uuid") uuid: UUID): Response {
        val episode = episodeService.find(uuid) ?: return Response.redirect(Link.EPISODES.href)

        return Response.template(
            "admin/episodes/edit.ftl",
            episode.anime?.name,
            mutableMapOf(
                "episode" to AbstractConverter.convert(episode, EpisodeDto::class.java),
                "episodeTypes" to EpisodeType.entries.toList(),
                "langTypes" to LangType.entries.toList(),
            )
        )
    }

    @Path("/{uuid}")
    @Post
    @AdminSessionAuthenticated
    private fun postEpisode(@PathParam("uuid") uuid: UUID, @BodyParam parameters: Parameters): Response {
        episodeService.update(uuid, parameters)
        return Response.redirect(Link.EPISODES.href)
    }

    @Path("/{uuid}/delete")
    @Get
    @AdminSessionAuthenticated
    private fun deleteEpisode(@PathParam("uuid") uuid: UUID): Response {
        episodeService.delete(episodeService.find(uuid) ?: return Response.redirect(Link.EPISODES.href))
        return Response.redirect(Link.EPISODES.href)
    }
}