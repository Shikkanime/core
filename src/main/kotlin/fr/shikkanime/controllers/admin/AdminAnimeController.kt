package fr.shikkanime.controllers.admin

import com.google.inject.Inject
import fr.shikkanime.converters.AbstractConverter
import fr.shikkanime.dtos.AnimeDto
import fr.shikkanime.entities.enums.Link
import fr.shikkanime.services.AnimeService
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

@Controller("/admin/animes")
class AdminAnimeController {
    @Inject
    private lateinit var animeService: AnimeService

    @Path
    @Get
    @AdminSessionAuthenticated
    private fun getAnimes(): Response {
        return Response.template(Link.ANIMES)
    }

    @Path("/{uuid}")
    @Get
    @AdminSessionAuthenticated
    private fun getAnimeView(@PathParam("uuid") uuid: UUID): Response {
        val anime = animeService.find(uuid) ?: return Response.redirect(Link.ANIMES.href)

        return Response.template(
            "admin/anime_view.ftl",
            anime.name,
            mutableMapOf("anime" to AbstractConverter.convert(anime, AnimeDto::class.java))
        )
    }

    @Path("/{uuid}")
    @Post
    @AdminSessionAuthenticated
    private fun postAnime(@PathParam("uuid") uuid: UUID, @BodyParam parameters: Parameters): Response {
        animeService.update(uuid, parameters)
        return Response.redirect(Link.ANIMES.href)
    }

    @Path("/{uuid}/delete")
    @Get
    @AdminSessionAuthenticated
    private fun deleteAnime(@PathParam("uuid") uuid: UUID): Response {
        animeService.delete(animeService.find(uuid) ?: return Response.redirect(Link.ANIMES.href))
        return Response.redirect(Link.ANIMES.href)
    }
}