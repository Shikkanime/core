package fr.shikkanime.controllers.api

import com.google.inject.Inject
import fr.shikkanime.converters.AbstractConverter
import fr.shikkanime.dtos.*
import fr.shikkanime.dtos.animes.DetailedAnimeDto
import fr.shikkanime.dtos.enums.Status
import fr.shikkanime.entities.enums.CountryCode
import fr.shikkanime.services.AnimeService
import fr.shikkanime.services.caches.AnimeCacheService
import fr.shikkanime.services.caches.MemberFollowAnimeCacheService
import fr.shikkanime.utils.routes.*
import fr.shikkanime.utils.routes.method.Delete
import fr.shikkanime.utils.routes.method.Get
import fr.shikkanime.utils.routes.method.Put
import fr.shikkanime.utils.routes.openapi.OpenAPI
import fr.shikkanime.utils.routes.openapi.OpenAPIResponse
import fr.shikkanime.utils.routes.param.BodyParam
import fr.shikkanime.utils.routes.param.PathParam
import fr.shikkanime.utils.routes.param.QueryParam
import java.time.DayOfWeek
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
    @OpenAPI(
        "Get animes",
        [
            OpenAPIResponse(
                200,
                "Animes found",
                PageableDto::class,
            ),
            OpenAPIResponse(
                409,
                "You can't use simulcast and name at the same time OR You can't use sort and desc with name",
                MessageDto::class
            ),
        ]
    )
    private fun getAll(
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

        return Response.ok(
            if (!name.isNullOrBlank()) {
                animeCacheService.findAllByName(name, countryParam, page, limit)
            } else {
                animeCacheService.findAllBy(countryParam, simulcastParam, sortParameters, page, limit, statusParam)
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
        return Response.ok(AbstractConverter.convert(animeService.findLoaded(uuid), DetailedAnimeDto::class.java))
    }

    @Path("/{uuid}")
    @Put
    @AdminSessionAuthenticated
    @OpenAPI(hidden = true)
    private fun updateAnime(@PathParam("uuid") uuid: UUID, @BodyParam detailedAnimeDto: DetailedAnimeDto): Response {
        val updated = animeService.update(uuid, detailedAnimeDto)
        return Response.ok(AbstractConverter.convert(updated, DetailedAnimeDto::class.java))
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
        uuid: UUID?,
        @QueryParam("country", description = "Country code to filter by", example = "FR", type = CountryCode::class)
        countryParam: String?,
        @QueryParam(
            "date",
            description = "Date to filter by. Format: yyyy-MM-dd",
            example = "2021-01-01"
        )
        dateParam: String?,
    ): Response {
        val parsedDate = try {
            dateParam?.let { LocalDate.parse(it, DateTimeFormatter.ofPattern("yyyy-MM-dd")) } ?: LocalDate.now()
        } catch (e: Exception) {
            return Response.badRequest(MessageDto(MessageDto.Type.ERROR, "Invalid week format"))
        }

        val startOfWeekDay = parsedDate.with(DayOfWeek.MONDAY)
        val countryCode = CountryCode.fromNullable(countryParam) ?: CountryCode.FR

        return Response.ok(animeCacheService.getWeeklyAnimes(uuid, startOfWeekDay, countryCode))
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