package fr.shikkanime.controllers.site

import com.google.inject.Inject
import fr.shikkanime.dtos.animes.AnimeDto
import fr.shikkanime.entities.enums.ConfigPropertyKey
import fr.shikkanime.entities.enums.CountryCode
import fr.shikkanime.entities.enums.EpisodeType
import fr.shikkanime.entities.enums.Link
import fr.shikkanime.entities.miscellaneous.SortParameter
import fr.shikkanime.services.caches.AnimeCacheService
import fr.shikkanime.services.caches.ConfigCacheService
import fr.shikkanime.services.caches.EpisodeMappingCacheService
import fr.shikkanime.services.caches.SimulcastCacheService
import fr.shikkanime.utils.StringUtils
import fr.shikkanime.utils.TelemetryConfig
import fr.shikkanime.utils.TelemetryConfig.span
import fr.shikkanime.utils.TelemetryConfig.spanWithAttributes
import fr.shikkanime.utils.atStartOfWeek
import fr.shikkanime.utils.routes.Controller
import fr.shikkanime.utils.routes.Path
import fr.shikkanime.utils.routes.Response
import fr.shikkanime.utils.routes.method.Get
import fr.shikkanime.utils.routes.param.PathParam
import fr.shikkanime.utils.routes.param.QueryParam
import java.time.LocalDate
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

@Controller("/")
class SiteController {
    private val tracer = TelemetryConfig.getTracer("SiteController")

    @Inject
    private lateinit var animeCacheService: AnimeCacheService

    @Inject
    private lateinit var episodeMappingCacheService: EpisodeMappingCacheService

    @Inject
    private lateinit var simulcastCacheService: SimulcastCacheService

    @Inject
    private lateinit var configCacheService: ConfigCacheService

    private fun getFullAnimesSimulcast(): MutableList<AnimeDto> {
        val animeSimulcastLimit = 6

        val animes = animeCacheService.findAllBy(
            CountryCode.FR,
            simulcastCacheService.currentSimulcast?.uuid,
            listOf(SortParameter("name", SortParameter.Order.ASC)),
            1,
            animeSimulcastLimit
        ).data.toMutableList()

        val simulcasts = simulcastCacheService.findAll()

        if (animes.size in 1..<animeSimulcastLimit && simulcasts.size > 1) {
            val previousSimulcastAnimes = animeCacheService.findAllBy(
                CountryCode.FR,
                simulcasts[1].uuid,
                listOf(SortParameter("name", SortParameter.Order.ASC)),
                1,
                animeSimulcastLimit - animes.size
            ).data

            animes.addAll(previousSimulcastAnimes)
        }

        return animes
    }

    @Path
    @Get
    private fun home() = tracer.span("GET /") {
        Response.template(
            Link.HOME,
            mutableMapOf(
                "animes" to getFullAnimesSimulcast(),
                "groupedEpisodes" to episodeMappingCacheService.findAllGroupedBy(
                    CountryCode.FR,
                    1,
                    8
                ).data,
            )
        )
    }

    @Path("catalog/{slug}")
    @Get
    private fun catalogSimulcast(
        @PathParam("slug")
        slug: String
    ) = tracer.spanWithAttributes("GET /catalog/${slug}") { span ->
        span.setAttribute("slug", slug)

        val findAll = simulcastCacheService.findAll()
        val selectedSimulcast = findAll.firstOrNull { it.slug == slug } ?: return@spanWithAttributes Response.notFound()

        span.setAttribute("simulcast", selectedSimulcast.uuid.toString())

        Response.template(
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
                ).data,
            )
        )
    }

    private fun getAnimeDetail(slug: String, season: Int? = null, page: Int? = null): Response {
        val dto = animeCacheService.findBySlug(CountryCode.FR, slug) ?: return Response.notFound()
        val seasonDto = dto.seasons?.firstOrNull { it.number == (season ?: it.number) } ?: return Response.notFound()
        val limit = configCacheService.getValueAsInt(ConfigPropertyKey.ANIME_EPISODES_SIZE_LIMIT, 24)
        val findAllBy = episodeMappingCacheService.findAllBy(
            CountryCode.FR,
            dto.uuid,
            seasonDto.number,
            listOf(
                SortParameter("releaseDateTime", SortParameter.Order.ASC),
                SortParameter("season", SortParameter.Order.ASC),
                SortParameter("episodeType", SortParameter.Order.ASC),
                SortParameter("number", SortParameter.Order.ASC),
            ),
            page ?: 1,
            limit
        )

        val title = dto.shortName + (season?.let { " - ${StringUtils.toSeasonString(dto.countryCode, it.toString())}" } ?: "")
        val showMore = ((((page ?: 1) - 1) * limit) + findAllBy.data.size < findAllBy.total.toInt())

        return Response.template(
            "/site/anime.ftl",
            title,
            mutableMapOf(
                "description" to dto.description,
                "anime" to dto,
                "season" to if (season != null) seasonDto else null,
                "episodeMappings" to findAllBy.data,
                "showMore" to showMore,
                "showLess" to ((page ?: 1) > 1),
                "page" to page,
            )
        )
    }

    @Path("animes/{slug}")
    @Get
    private fun animeDetail(
        @PathParam("slug") slug: String
    ) = tracer.spanWithAttributes("GET /animes/${slug}") { span ->
        span.setAttribute("slug", slug)
        getAnimeDetail(slug)
    }

    @Path("animes/{slug}/season-{season}")
    @Get
    private fun animeDetailBySeason(
        @PathParam("slug") slug: String,
        @PathParam("season") season: Int,
        @QueryParam("page") page: Int?
    ) = tracer.spanWithAttributes("GET /animes/${slug}/season-${season}") { span ->
        span.setAttribute("slug", slug)
        span.setAttribute("season", season.toString())

        getAnimeDetail(slug, season, page)
    }

    @Path("animes/{slug}/season-{season}/{episodeSlug}")
    @Get
    private fun episodeDetails(
        @PathParam("slug") slug: String,
        @PathParam("season") season: Int,
        @PathParam("episodeSlug") episodeSlug: String
    ): Response {
        return tracer.spanWithAttributes("GET /animes/${slug}/season-${season}/$episodeSlug") { span ->
            span.setAttribute("slug", slug)
            span.setAttribute("season", season.toString())
            span.setAttribute("episodeSlug", episodeSlug)

            val dto = animeCacheService.findBySlug(CountryCode.FR, slug) ?: return@spanWithAttributes Response.notFound()
            if (dto.seasons.isNullOrEmpty()) return@spanWithAttributes Response.notFound()
            if (dto.seasons!!.none { it.number == season }) return@spanWithAttributes Response.redirect("/animes/$slug/season-${dto.seasons!!.last().number}/$episodeSlug")

            val match = "(${EpisodeType.entries.joinToString("|") { it.slug }})-(-?\\d+)".toRegex().find(episodeSlug) ?: return@spanWithAttributes Response.notFound()
            val episodeType = EpisodeType.fromSlug(match.groupValues[1])
            val episodeNumber = match.groupValues[2].toInt()

            val (previousDto, currentDto, nextDto) = episodeMappingCacheService.findPreviousAndNextBy(
                dto.uuid!!,
                season,
                episodeType,
                episodeNumber
            ) ?: return@spanWithAttributes Response.notFound()

            val title =
                currentDto.anime!!.shortName + " - ${StringUtils.toEpisodeMappingString(currentDto, separator = false)}"

            return@spanWithAttributes Response.template(
                "/site/episodeDetails.ftl",
                title,
                mutableMapOf(
                    "description" to currentDto.description,
                    "episodeMapping" to currentDto,
                    "previousEpisode" to previousDto,
                    "nextEpisode" to nextDto,
                )
            )
        }
    }

    @Path("search")
    @Get
    private fun search(
        @QueryParam("q") query: String?,
        @QueryParam("searchTypes") searchTypes: String?,
        @QueryParam("page") pageParam: Int?,
    ) = tracer.spanWithAttributes("GET /search") { span ->
        span.setAttribute("query", query ?: "")
        span.setAttribute("searchTypes", searchTypes ?: "")
        span.setAttribute("page", pageParam?.toLong() ?: 1)

        Response.template(
            Link.SEARCH,
            mutableMapOf(
                "query" to query,
                "searchTypes" to searchTypes,
                "page" to pageParam,
            )
        )
    }

    @Path("calendar")
    @Get
    private fun calendar(@QueryParam("date") date: String?) = tracer.spanWithAttributes("GET /calendar") { span ->
        span.setAttribute("date", date ?: "")

        val startOfWeekDay = try {
            date?.let { LocalDate.parse(it, DateTimeFormatter.ISO_LOCAL_DATE) } ?: LocalDate.now()
        } catch (_: Exception) {
            LocalDate.now()
        }.atStartOfWeek()

        if (startOfWeekDay < episodeMappingCacheService.findMinimalReleaseDateTime().toLocalDate().atStartOfWeek())
            return@spanWithAttributes Response.notFound()

        Response.template(
            Link.CALENDAR,
            mutableMapOf(
                "weeklyAnimes" to animeCacheService.getWeeklyAnimes(CountryCode.FR, null, startOfWeekDay),
                "previousWeek" to startOfWeekDay.minusDays(7),
                "nextWeek" to startOfWeekDay.plusDays(7).takeIf { it <= ZonedDateTime.now().toLocalDate() }
            )
        )
    }

    @Path("presentation")
    @Get
    private fun presentation(): Response {
        return Response.template(Link.PRESENTATION)
    }

    @Path("privacy")
    @Get
    private fun privacy(): Response {
        return Response.template(Link.PRIVACY)
    }
}