package fr.shikkanime.controllers.site

import com.google.inject.Inject
import fr.shikkanime.entities.SortParameter
import fr.shikkanime.entities.enums.CountryCode
import fr.shikkanime.entities.enums.Link
import fr.shikkanime.services.caches.AnimeCacheService
import fr.shikkanime.services.caches.EpisodeCacheService
import fr.shikkanime.services.caches.SimulcastCacheService
import fr.shikkanime.utils.routes.Controller
import fr.shikkanime.utils.routes.Path
import fr.shikkanime.utils.routes.Response
import fr.shikkanime.utils.routes.method.Get
import fr.shikkanime.utils.routes.param.PathParam
import io.ktor.http.*

@Controller("/")
class SiteController {
    @Inject
    private lateinit var animeCacheService: AnimeCacheService

    @Inject
    private lateinit var episodeCacheService: EpisodeCacheService

    @Inject
    private lateinit var simulcastCacheService: SimulcastCacheService

    @Path("404")
    @Get
    private fun error404(): Response {
        return Response.template(
            HttpStatusCode.NotFound,
            "/site/404.ftl",
            "Page introuvable"
        )
    }

    @Path
    @Get
    private fun home(): Response {
        return Response.template(
            Link.HOME,
            mutableMapOf(
                "animes" to animeCacheService.findAllBy(
                    CountryCode.FR,
                    simulcastCacheService.currentSimulcast?.uuid,
                    listOf(SortParameter("name", SortParameter.Order.ASC)),
                    1,
                    6
                )!!.data,
                "episodes" to episodeCacheService.findAllBy(
                    CountryCode.FR,
                    null,
                    listOf(
                        SortParameter("releaseDateTime", SortParameter.Order.DESC),
                        SortParameter("season", SortParameter.Order.DESC),
                        SortParameter("number", SortParameter.Order.DESC),
                        SortParameter("episodeType", SortParameter.Order.ASC),
                        SortParameter("langType", SortParameter.Order.ASC),
                    ),
                    1,
                    6
                )!!.data
            )
        )
    }

    @Path("catalog/{slug}")
    @Get
    private fun catalogSimulcast(@PathParam("slug") slug: String): Response {
        val findAll = simulcastCacheService.findAll()!!
        val selectedSimulcast = findAll.firstOrNull { it.slug == slug } ?: return Response.redirect("/404")

        return Response.template(
            Link.CATALOG.template,
            selectedSimulcast.label,
            mutableMapOf(
                "simulcasts" to findAll,
                "selectedSimulcast" to selectedSimulcast,
                "animes" to animeCacheService.findAllBy(
                    CountryCode.FR,
                    selectedSimulcast.uuid,
                    listOf(SortParameter("name", SortParameter.Order.ASC)),
                    1,
                    102
                )!!.data,
            )
        )
    }

    @Path("animes/{slug}")
    @Get
    private fun animeDetail(@PathParam("slug") slug: String): Response {
        val anime = animeCacheService.findBySlug(slug) ?: return Response.redirect("/404")

        return Response.template(
            "/site/anime.ftl",
            anime.shortName,
            mutableMapOf(
                "description" to anime.description,
                "anime" to anime,
                "episodes" to episodeCacheService.findAllBy(
                    CountryCode.FR,
                    anime.uuid,
                    listOf(
                        SortParameter("season", SortParameter.Order.ASC),
                        SortParameter("number", SortParameter.Order.ASC),
                        SortParameter("episodeType", SortParameter.Order.ASC),
                        SortParameter("langType", SortParameter.Order.ASC),
                    ),
                    1,
                    24
                )!!.data
            )
        )
    }

    @Path("presentation")
    @Get
    private fun presentation(): Response {
        return Response.template(Link.PRESENTATION)
    }
}