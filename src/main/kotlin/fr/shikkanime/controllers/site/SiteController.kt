package fr.shikkanime.controllers.site

import com.google.inject.Inject
import fr.shikkanime.entities.SortParameter
import fr.shikkanime.entities.enums.ConfigPropertyKey
import fr.shikkanime.entities.enums.CountryCode
import fr.shikkanime.entities.enums.Link
import fr.shikkanime.services.caches.AnimeCacheService
import fr.shikkanime.services.caches.ConfigCacheService
import fr.shikkanime.services.caches.EpisodeCacheService
import fr.shikkanime.services.caches.SimulcastCacheService
import fr.shikkanime.utils.routes.Controller
import fr.shikkanime.utils.routes.Path
import fr.shikkanime.utils.routes.Response
import fr.shikkanime.utils.routes.method.Get
import fr.shikkanime.utils.routes.param.PathParam
import java.util.*

@Controller("/")
class SiteController {
    @Inject
    private lateinit var animeCacheService: AnimeCacheService

    @Inject
    private lateinit var episodeCacheService: EpisodeCacheService

    @Inject
    private lateinit var configCacheService: ConfigCacheService

    @Inject
    private lateinit var simulcastCacheService: SimulcastCacheService

    @Path
    @Get
    private fun home(): Response {
        return Response.template(
            Link.HOME,
            mutableMapOf(
                "description" to configCacheService.getValueAsString(ConfigPropertyKey.SEO_DESCRIPTION),
                "animes" to animeCacheService.findAllBy(
                    CountryCode.FR,
                    null,
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

    @Path("catalog")
    @Get
    private fun catalog(): Response {
        val findAll = simulcastCacheService.findAll()!!
        val currentSimulcast = findAll.first()

        return Response.template(
            Link.CATALOG,
            mutableMapOf(
                "description" to configCacheService.getValueAsString(ConfigPropertyKey.SEO_DESCRIPTION),
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

    @Path("catalog/{uuid}")
    @Get
    private fun catalogSimulcast(@PathParam("uuid") uuid: UUID): Response {
        val findAll = simulcastCacheService.findAll()!!
        val currentSimulcast = findAll.first { it.uuid == uuid }

        return Response.template(
            Link.CATALOG,
            mutableMapOf(
                "description" to configCacheService.getValueAsString(ConfigPropertyKey.SEO_DESCRIPTION),
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
            "/site/404.ftl",
            "Page introuvable",
        )
    }

    @Path("animes/{uuid}")
    @Get
    private fun animeDetail(@PathParam("uuid") uuid: UUID): Response {
        val anime = animeCacheService.find(uuid) ?: return Response.redirect("/404")

        return Response.template(
            "/site/anime.ftl",
            anime.shortName,
            mutableMapOf(
                "description" to anime.description,
                "anime" to anime,
                "episodes" to episodeCacheService.findAllBy(
                    CountryCode.FR,
                    uuid,
                    listOf(
                        SortParameter("season", SortParameter.Order.ASC),
                        SortParameter("number", SortParameter.Order.ASC),
                        SortParameter("episodeType", SortParameter.Order.ASC),
                        SortParameter("langType", SortParameter.Order.ASC),
                    ),
                    1,
                    12
                )!!.data
            )
        )
    }
}