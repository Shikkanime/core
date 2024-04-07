package fr.shikkanime.controllers.site

import com.google.inject.Inject
import fr.shikkanime.dtos.animes.AnimeDto
import fr.shikkanime.entities.SortParameter
import fr.shikkanime.entities.enums.ConfigPropertyKey
import fr.shikkanime.entities.enums.CountryCode
import fr.shikkanime.entities.enums.Link
import fr.shikkanime.services.caches.AnimeCacheService
import fr.shikkanime.services.caches.ConfigCacheService
import fr.shikkanime.services.caches.EpisodeCacheService
import fr.shikkanime.services.caches.SimulcastCacheService
import fr.shikkanime.utils.StringUtils
import fr.shikkanime.utils.routes.Controller
import fr.shikkanime.utils.routes.Path
import fr.shikkanime.utils.routes.Response
import fr.shikkanime.utils.routes.method.Get
import fr.shikkanime.utils.routes.param.PathParam
import fr.shikkanime.utils.routes.param.QueryParam
import io.ktor.http.*
import java.time.ZonedDateTime

@Controller("/")
class SiteController {
    @Inject
    private lateinit var animeCacheService: AnimeCacheService

    @Inject
    private lateinit var episodeCacheService: EpisodeCacheService

    @Inject
    private lateinit var simulcastCacheService: SimulcastCacheService

    @Inject
    private lateinit var configCacheService: ConfigCacheService

    @Path("404")
    @Get
    private fun error404(): Response {
        return Response.template(
            HttpStatusCode.NotFound,
            "/site/404.ftl",
            "Page introuvable"
        )
    }

    private fun getFullAnimesSimulcast(): MutableList<AnimeDto> {
        val animeSimulcastLimit = 6

        val animes = animeCacheService.findAllBy(
            CountryCode.FR,
            simulcastCacheService.currentSimulcast?.uuid,
            listOf(SortParameter("name", SortParameter.Order.ASC)),
            1,
            animeSimulcastLimit
        )!!.data.toMutableList()

        val simulcasts = simulcastCacheService.findAll()

        if (animes.size in 1..<animeSimulcastLimit && simulcasts!!.size > 1) {
            val previousSimulcastAnimes = animeCacheService.findAllBy(
                CountryCode.FR,
                simulcasts[1].uuid,
                listOf(SortParameter("name", SortParameter.Order.ASC)),
                1,
                animeSimulcastLimit - animes.size
            )!!.data

            animes.addAll(previousSimulcastAnimes)
        }

        return animes
    }

    @Path
    @Get
    private fun home(): Response {
        return Response.template(
            Link.HOME,
            mutableMapOf(
                "animes" to getFullAnimesSimulcast(),
                "episodes" to episodeCacheService.findAllBy(
                    CountryCode.FR,
                    null,
                    listOf(
                        SortParameter("releaseDateTime", SortParameter.Order.DESC),
                        SortParameter("season", SortParameter.Order.DESC),
                        SortParameter("episodeType", SortParameter.Order.DESC),
                        SortParameter("number", SortParameter.Order.DESC),
                        SortParameter("langType", SortParameter.Order.ASC),
                    ),
                    1,
                    8
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
                "description" to anime.description?.let { StringUtils.sanitizeXSS(it) },
                "anime" to anime,
                "episodes" to episodeCacheService.findAllBy(
                    CountryCode.FR,
                    anime.uuid,
                    listOf(
                        SortParameter("season", SortParameter.Order.ASC),
                        SortParameter("episodeType", SortParameter.Order.DESC),
                        SortParameter("number", SortParameter.Order.ASC),
                        SortParameter("langType", SortParameter.Order.ASC),
                    ),
                    1,
                    configCacheService.getValueAsInt(ConfigPropertyKey.ANIME_EPISODES_SIZE_LIMIT, 24)
                )!!.data
            )
        )
    }

    @Path("search")
    @Get
    private fun search(
        @QueryParam("q") query: String?,
    ): Response {
        return Response.template(
            Link.SEARCH,
            mutableMapOf(
                "query" to query
            )
        )
    }

    @Path("calendar")
    @Get
    private fun calendar(): Response {
        val now = ZonedDateTime.now().toLocalDate()

        return Response.template(
            Link.CALENDAR,
            mutableMapOf(
                "weeklyAnimes" to animeCacheService.getWeeklyAnimes(
                    now.minusDays(now.dayOfWeek.value.toLong() - 1),
                    CountryCode.FR
                ),
            )
        )
    }

    @Path("presentation")
    @Get
    private fun presentation(): Response {
        return Response.template(Link.PRESENTATION)
    }
}