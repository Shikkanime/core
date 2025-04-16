package fr.shikkanime.controllers.site

import com.google.inject.Inject
import fr.shikkanime.dtos.animes.AnimeDto
import fr.shikkanime.entities.enums.*
import fr.shikkanime.entities.miscellaneous.SortParameter
import fr.shikkanime.services.caches.*
import fr.shikkanime.utils.StringUtils
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
    @Inject private lateinit var animeCacheService: AnimeCacheService
    @Inject private lateinit var episodeMappingCacheService: EpisodeMappingCacheService
    @Inject private lateinit var simulcastCacheService: SimulcastCacheService
    @Inject private lateinit var configCacheService: ConfigCacheService
    @Inject private lateinit var groupedEpisodeCacheService: GroupedEpisodeCacheService

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
    private fun home(): Response {
        return Response.template(
            Link.HOME,
            mutableMapOf(
                "animes" to getFullAnimesSimulcast(),
                "groupedEpisodes" to groupedEpisodeCacheService.findAllBy(
                    CountryCode.FR,
                    1,
                    8
                ).data,
            )
        )
    }

    @Path("catalog/{slug}")
    @Get
    private fun catalogSimulcast(@PathParam("slug") slug: String): Response {
        val findAll = simulcastCacheService.findAll()
        val selectedSimulcast = findAll.firstOrNull { it.slug == slug } ?: return Response.notFound()

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
    ) = getAnimeDetail(slug)

    @Path("animes/{slug}/season-{season}")
    @Get
    private fun animeDetailBySeason(
        @PathParam("slug") slug: String,
        @PathParam("season") season: Int,
        @QueryParam("page") page: Int?
    ) = getAnimeDetail(slug, season, page)

    @Path("animes/{slug}/season-{season}/{episodeSlug}")
    @Get
    private fun episodeDetails(
        @PathParam("slug") slug: String,
        @PathParam("season") season: Int,
        @PathParam("episodeSlug") episodeSlug: String
    ): Response {
        val dto = animeCacheService.findBySlug(CountryCode.FR, slug) ?: return Response.notFound()
        if (dto.seasons.isNullOrEmpty()) return Response.notFound()
        if (dto.seasons!!.none { it.number == season }) return Response.redirect("/animes/$slug/season-${dto.seasons!!.last().number}/$episodeSlug")

        val match = "(${EpisodeType.entries.joinToString("|") { it.slug }})-(-?\\d+)".toRegex().find(episodeSlug) ?: return Response.notFound()
        val episodeType = EpisodeType.fromSlug(match.groupValues[1])
        val episodeNumber = match.groupValues[2].toInt()

        val (previousDto, currentDto, nextDto) = episodeMappingCacheService.findPreviousAndNextBy(
            dto.uuid!!,
            season,
            episodeType,
            episodeNumber
        ) ?: return Response.notFound()

        val title =
            currentDto.anime!!.shortName + " - ${StringUtils.toEpisodeMappingString(currentDto, separator = false)}"

        return Response.template(
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

    @Path("search")
    @Get
    private fun search(
        @QueryParam("q") query: String?,
        @QueryParam("searchTypes") searchTypes: String?,
        @QueryParam("page") pageParam: Int?,
    ): Response {
        return Response.template(
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
    private fun calendar(
        @QueryParam("date") date: String?,
        @QueryParam("searchTypes") searchTypes: Array<LangType>?,
    ): Response {
        val startOfWeekDay = try {
            date?.let { LocalDate.parse(it, DateTimeFormatter.ISO_LOCAL_DATE) } ?: LocalDate.now()
        } catch (_: Exception) {
            LocalDate.now()
        }.atStartOfWeek()

        val minimalReleaseDate = episodeMappingCacheService.findMinimalReleaseDateTime().toLocalDate().atStartOfWeek()

        if (startOfWeekDay < minimalReleaseDate)
            return Response.notFound()

        return Response.template(
            Link.CALENDAR,
            mutableMapOf(
                "weeklyAnimes" to animeCacheService.getWeeklyAnimes(CountryCode.FR, null, startOfWeekDay, searchTypes),
                "previousWeek" to startOfWeekDay.minusDays(7).takeIf { it >= minimalReleaseDate },
                "searchTypes" to searchTypes.orEmpty().joinToString(","),
                "currentWeek" to startOfWeekDay,
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