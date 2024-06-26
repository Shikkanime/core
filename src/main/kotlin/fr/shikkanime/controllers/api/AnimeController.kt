package fr.shikkanime.controllers.api

import com.google.inject.Inject
import fr.shikkanime.converters.AbstractConverter
import fr.shikkanime.dtos.*
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
        @QueryParam("name") name: String?,
        @QueryParam("country", description = "By default: FR", type = CountryCode::class) countryParam: CountryCode?,
        @QueryParam("simulcast") simulcastParam: UUID?,
        @QueryParam("page") pageParam: Int?,
        @QueryParam("limit") limitParam: Int?,
        @QueryParam("sort") sortParam: String?,
        @QueryParam("desc") descParam: String?,
        @QueryParam("status") statusParam: Status?,
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
        return Response.ok(AbstractConverter.convert(animeService.find(uuid), AnimeDto::class.java))
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
            OpenAPIResponse(
                200,
                "Weekly anime found",
                Array<WeeklyAnimesDto>::class,
            ),
            OpenAPIResponse(
                400,
                "Invalid week format",
                MessageDto::class
            ),
            OpenAPIResponse(401, "Unauthorized")
        ],
        security = true
    )
    private fun getWeekly(
        @JWTUser uuid: UUID?,
        @QueryParam("country", description = "By default: FR", type = CountryCode::class) countryParam: String?,
        @QueryParam("date", description = "By default: today", type = String::class) dateParam: String?,
    ): Response {
        val parsedDate = if (dateParam != null) {
            val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

            try {
                LocalDate.parse(dateParam, formatter)
            } catch (e: Exception) {
                return Response.badRequest(
                    MessageDto(
                        MessageDto.Type.ERROR,
                        "Invalid week format",
                    )
                )
            }
        } else {
            LocalDate.now()
        }

        return Response.ok(
            animeCacheService.getWeeklyAnimes(
                uuid,
                parsedDate!!.minusDays(parsedDate.dayOfWeek.value.toLong() - 1),
                CountryCode.fromNullable(countryParam) ?: CountryCode.FR
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
        @QueryParam("page") pageParam: Int?,
        @QueryParam("limit") limitParam: Int?,
    ): Response {
        val (page, limit, _) = pageableRoute(pageParam, limitParam, null, null)

        return Response.ok(
            memberFollowAnimeCacheService.getMissedAnimes(uuid, page, limit) ?: return Response.notFound()
        )
    }
}