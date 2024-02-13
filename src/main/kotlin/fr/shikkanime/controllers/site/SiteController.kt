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

    @Path
    @Get
    private fun home(): Response {
        val findAll = simulcastCacheService.findAll()!!
        val currentSimulcast = findAll.first()

        return Response.template(
            Link.HOME,
            mutableMapOf(
                "animes" to animeCacheService.findAllBy(
                    CountryCode.FR,
                    currentSimulcast.uuid,
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

    @Path("robots.txt")
    @Get
    private fun robots(): Response {
        return Response.template(
            "/site/seo/robots.ftl",
            null,
            contentType = ContentType.Text.Plain
        )
    }

    @Path("sitemap.xml")
    @Get
    private fun sitemap(): Response {
        val simulcasts = simulcastCacheService.findAllUpdated()!!

        val animes = simulcasts.flatMap {
            animeCacheService.findAllByUpdated(
                CountryCode.FR,
                it.uuid,
                listOf(SortParameter("name", SortParameter.Order.ASC)),
                1,
                102
            )!!.data
        }.distinctBy { it.uuid }

        val episode = episodeCacheService.findAllBy(
            CountryCode.FR,
            null,
            listOf(SortParameter("releaseDateTime", SortParameter.Order.DESC)),
            1,
            1
        )!!.data.first()

        return Response.template(
            "/site/seo/sitemap.ftl",
            null,
            mutableMapOf(
                "episode" to episode,
                "simulcastsUpdated" to simulcasts,
                "animesUpdated" to animes
            ),
            contentType = ContentType.Text.Xml
        )
    }

    @Path("catalog")
    @Get
    private fun catalog(): Response {
        val findAll = simulcastCacheService.findAll()!!
        val currentSimulcast = findAll.first()
        return Response.redirect("/catalog/${currentSimulcast.slug}")
    }

    @Path("catalog/{slug}")
    @Get
    private fun catalogSimulcast(@PathParam("slug") slug: String): Response {
        val findAll = simulcastCacheService.findAll()!!
        val currentSimulcast = findAll.firstOrNull { it.slug == slug } ?: return Response.redirect("/404")

        return Response.template(
            Link.CATALOG.template,
            currentSimulcast.label,
            mutableMapOf(
                "simulcasts" to findAll,
                "currentSimulcast" to currentSimulcast,
                "animes" to animeCacheService.findAllBy(
                    CountryCode.FR,
                    currentSimulcast.uuid,
                    listOf(SortParameter("name", SortParameter.Order.ASC)),
                    1,
                    102
                )!!.data,
            )
        )
    }

    @Path("404")
    @Get
    private fun error404(): Response {
        return Response.template(
            HttpStatusCode.NotFound,
            "/site/404.ftl",
            "Page introuvable"
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
}