package fr.shikkanime.controllers.api

import com.google.inject.Inject
import fr.shikkanime.converters.AbstractConverter
import fr.shikkanime.dtos.*
import fr.shikkanime.dtos.animes.AnimeDto
import fr.shikkanime.dtos.enums.Status
import fr.shikkanime.dtos.weekly.v1.WeeklyAnimesDto
import fr.shikkanime.entities.enums.CountryCode
import fr.shikkanime.entities.enums.LangType
import fr.shikkanime.services.AnimeService
import fr.shikkanime.services.caches.AnimeCacheService
import fr.shikkanime.services.caches.MemberFollowAnimeCacheService
import fr.shikkanime.utils.atStartOfWeek
import fr.shikkanime.utils.routes.*
import fr.shikkanime.utils.routes.method.Delete
import fr.shikkanime.utils.routes.method.Get
import fr.shikkanime.utils.routes.method.Put
import fr.shikkanime.utils.routes.openapi.OpenAPI
import fr.shikkanime.utils.routes.openapi.OpenAPIResponse
import fr.shikkanime.utils.routes.param.BodyParam
import fr.shikkanime.utils.routes.param.PathParam
import fr.shikkanime.utils.routes.param.QueryParam
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.*

@Controller("/api/v1/animes")
class AnimeController : HasPageableRoute() {
    @Inject
    private lateinit var animeCacheService: AnimeCacheService

    @Inject
    private lateinit var animeService: AnimeService

    @Inject
    private lateinit var memberFollowAnimeCacheService: MemberFollowAnimeCacheService

    @Path
    @Get
    @JWTAuthenticated(optional = true)
    @OpenAPI(
        "Get animes",
        [
            OpenAPIResponse(
                200,
                "Animes found",
                PageableDto::class,
            ),
            OpenAPIResponse(401, "Unauthorized"),
            OpenAPIResponse(
                409,
                "You can't use simulcast and name at the same time OR You can't use sort and desc with name",
                MessageDto::class
            ),
        ],
        security = true
    )
    private fun getAll(
        @JWTUser
        memberUuid: UUID?,
        @QueryParam("name", description = "Name to filter by")
        name: String?,
        @QueryParam("country", description = "Country code to filter by", example = "FR", type = CountryCode::class)
        countryParam: CountryCode?,
        @QueryParam("simulcast", description = "UUID of the simulcast to filter by", type = UUID::class)
        simulcastParam: UUID?,
        @QueryParam("page", description = "Page number for pagination")
        pageParam: Int?,
        @QueryParam("limit", description = "Number of items per page. Must be between 1 and 30", example = "15")
        limitParam: Int?,
        @QueryParam(
            "sort",
            description = "Comma separated list of fields\n" +
                    "\n" +
                    "Possible values:\n" +
                    "- name\n" +
                    "- releaseDateTime\n" +
                    "- lastReleaseDateTime",
            example = "name"
        )
        sortParam: String?,
        @QueryParam(
            "desc",
            description = "A comma-separated list of fields to sort in descending order",
        )
        descParam: String?,
        @QueryParam("status", description = "Status to filter by", type = Status::class)
        statusParam: Status?,
        @QueryParam("searchTypes", description = "Search types to filter by", type = LangType::class)
        searchTypes: Array<LangType>?,
    ): Response {
        if (simulcastParam != null && name != null) {
            return Response.conflict(
                MessageDto(
                    MessageDto.Type.ERROR,
                    "You can't use simulcast and name at the same time",
                )
            )
        }

        if (name != null && (sortParam != null || descParam != null)) {
            return Response.conflict(
                MessageDto(
                    MessageDto.Type.ERROR,
                    "You can't use sort and desc with name",
                )
            )
        }

        val (page, limit, sortParameters) = pageableRoute(pageParam, limitParam, sortParam, descParam)

        if (memberUuid != null) {
            return Response.ok(
                memberFollowAnimeCacheService.findAllBy(
                    memberUuid,
                    page,
                    limit
                )
            )
        }

        return Response.ok(
            if (!name.isNullOrBlank()) {
                animeCacheService.findAllByName(countryParam, name, page, limit, searchTypes)
            } else {
                animeCacheService.findAllBy(
                    countryParam,
                    simulcastParam,
                    sortParameters,
                    page,
                    limit,
                    searchTypes,
                    statusParam
                )
            }
        )
    }

    @Path("/{uuid}")
    @Get
    @AdminSessionAuthenticated
    @OpenAPI(hidden = true)
    private fun animeDetails(
        @PathParam("uuid") uuid: UUID,
    ): Response {
        return Response.ok(AbstractConverter.convert(animeService.find(uuid) ?: return Response.notFound(), AnimeDto::class.java))
    }

    @Path("/{uuid}")
    @Put
    @AdminSessionAuthenticated
    @OpenAPI(hidden = true)
    private fun updateAnime(@PathParam("uuid") uuid: UUID, @BodyParam animeDto: AnimeDto): Response {
        val updated = animeService.update(uuid, animeDto)
        return Response.ok(AbstractConverter.convert(updated, AnimeDto::class.java))
    }

    @Path("/{uuid}")
    @Delete
    @AdminSessionAuthenticated
    @OpenAPI(hidden = true)
    private fun deleteAnime(@PathParam("uuid") uuid: UUID): Response {
        animeService.delete(animeService.find(uuid) ?: return Response.notFound())
        return Response.noContent()
    }

    @Path("/weekly")
    @Get
    @JWTAuthenticated(optional = true)
    @OpenAPI(
        "Get weekly anime",
        [
            OpenAPIResponse(200, "Weekly anime found", Array<WeeklyAnimesDto>::class),
            OpenAPIResponse(400, "Invalid week format", MessageDto::class),
            OpenAPIResponse(401, "Unauthorized")
        ],
        security = true
    )
    private fun getWeekly(
        @JWTUser
        memberUuid: UUID?,
        @QueryParam("country", description = "Country code to filter by", example = "FR", type = CountryCode::class)
        countryParam: CountryCode?,
        @QueryParam(
            "date",
            description = "Date to filter by. Format: yyyy-MM-dd",
            example = "2021-01-01"
        )
        dateParam: String?,
    ): Response {
        val startOfWeekDay = try {
            dateParam?.let { LocalDate.parse(it, DateTimeFormatter.ISO_LOCAL_DATE) } ?: LocalDate.now()
        } catch (_: Exception) {
            return Response.badRequest(MessageDto(MessageDto.Type.ERROR, "Invalid week format"))
        }.atStartOfWeek()

        return Response.ok(
            animeCacheService.getWeeklyAnimes(
                countryParam ?: CountryCode.FR,
                memberUuid,
                startOfWeekDay,
            )
        )
    }

    @Path("/missed")
    @Get
    @JWTAuthenticated
    @OpenAPI(
        "Get missed animes",
        [
            OpenAPIResponse(
                200,
                "Get missed animes",
                PageableDto::class,
            ),
            OpenAPIResponse(401, "Unauthorized")
        ],
        security = true
    )
    private fun getMissedAnimes(
        @JWTUser uuid: UUID,
        @QueryParam("page", description = "Page number for pagination")
        pageParam: Int?,
        @QueryParam("limit", description = "Number of items per page. Must be between 1 and 30", example = "15")
        limitParam: Int?,
    ): Response {
        val (page, limit, _) = pageableRoute(pageParam, limitParam, null, null)

        return Response.ok(
            memberFollowAnimeCacheService.getMissedAnimes(uuid, page, limit) ?: return Response.notFound()
        )
    }
}