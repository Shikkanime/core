package fr.shikkanime.controllers.api

import com.google.inject.Inject
import fr.shikkanime.dtos.MessageDto
import fr.shikkanime.dtos.enums.Status
import fr.shikkanime.entities.enums.CountryCode
import fr.shikkanime.entities.enums.LangType
import fr.shikkanime.services.caches.AnimeCacheService
import fr.shikkanime.services.caches.MemberFollowAnimeCacheService
import fr.shikkanime.utils.atStartOfWeek
import fr.shikkanime.utils.routes.*
import fr.shikkanime.utils.routes.method.Get
import fr.shikkanime.utils.routes.param.QueryParam
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.*

@Controller("/api/v1/animes")
class AnimeController : HasPageableRoute() {
    @Inject
    private lateinit var animeCacheService: AnimeCacheService

    @Inject
    private lateinit var memberFollowAnimeCacheService: MemberFollowAnimeCacheService

    @Path
    @Get
    @JWTAuthenticated(optional = true)
    private fun getAll(
        @JWTUser
        memberUuid: UUID?,
        @QueryParam("name")
        name: String?,
        @QueryParam("country")
        countryParam: CountryCode?,
        @QueryParam("simulcast")
        simulcastParam: UUID?,
        @QueryParam("page")
        pageParam: Int?,
        @QueryParam("limit")
        limitParam: Int?,
        @QueryParam("sort")
        sortParam: String?,
        @QueryParam("desc")
        descParam: String?,
        @QueryParam("status")
        statusParam: Status?,
        @QueryParam("searchTypes")
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

    @Path("/weekly")
    @Get
    @JWTAuthenticated(optional = true)
    fun getWeekly(
        @JWTUser
        memberUuid: UUID?,
        @QueryParam("country")
        countryParam: CountryCode?,
        @QueryParam("date")
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
    private fun getMissedAnimes(
        @JWTUser uuid: UUID,
        @QueryParam("page")
        pageParam: Int?,
        @QueryParam("limit")
        limitParam: Int?,
    ): Response {
        val (page, limit, _) = pageableRoute(pageParam, limitParam, null, null)
        return Response.ok(memberFollowAnimeCacheService.getMissedAnimes(uuid, page, limit))
    }
}