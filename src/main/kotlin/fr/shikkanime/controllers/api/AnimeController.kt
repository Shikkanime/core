package fr.shikkanime.controllers.api

import com.google.inject.Inject
import fr.shikkanime.dtos.MessageDto
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
    @Inject private lateinit var animeCacheService: AnimeCacheService
    @Inject private lateinit var memberFollowAnimeCacheService: MemberFollowAnimeCacheService

    @Path
    @Get
    @JWTAuthenticated(optional = true)
    private fun getAll(
        @JWTUser memberUuid: UUID?,
        @QueryParam parameters: Map<String, String>
    ): Response {
        val countryCode = CountryCode.fromNullable(parameters["country"])
        val simulcastUuid = parameters["simulcast"]?.let(UUID::fromString)
        val name = parameters["name"]
        val sort = parameters["sort"]
        val desc = parameters["desc"]
        val searchTypes = parameters["searchTypes"]?.split(",")?.map(LangType::valueOf)?.toTypedArray()

        if (simulcastUuid != null && name != null)
            return Response.conflict(MessageDto.error("You can't use simulcast and name at the same time"))
        if (name != null && (sort != null || desc != null))
            return Response.conflict(MessageDto.error("You can't use sort and desc with name"))

        val (page, limit, sortParams) = pageableRoute(
            parameters["page"]?.toIntOrNull(),
            parameters["limit"]?.toIntOrNull() ?: 9,
            sort,
            desc
        )

        return Response.ok(
            if (memberUuid != null) {
                memberFollowAnimeCacheService.findAllBy(memberUuid, page, limit)
            } else {
                if (!name.isNullOrBlank()) {
                    animeCacheService.findAllByName(countryCode, name, page, limit, searchTypes)
                } else {
                    animeCacheService.findAllBy(countryCode, simulcastUuid, sortParams, page, limit, searchTypes)
                }
            }
        )
    }

    @Path("/weekly")
    @Get
    @JWTAuthenticated(optional = true)
    fun getWeekly(
        @JWTUser memberUuid: UUID?,
        @QueryParam(defaultValue = "FR") country: CountryCode,
        @QueryParam date: String?,
        @QueryParam searchTypes: Array<LangType>?
    ): Response {
        val startOfWeekDay = try {
            date?.let { LocalDate.parse(it, DateTimeFormatter.ISO_LOCAL_DATE) } ?: LocalDate.now()
        } catch (_: Exception) {
            return Response.badRequest(MessageDto.error("Invalid week format"))
        }.atStartOfWeek()

        return Response.ok(animeCacheService.getWeeklyAnimes(country, memberUuid, startOfWeekDay, searchTypes))
    }

    @Path("/missed")
    @Get
    @JWTAuthenticated
    private fun getMissedAnimes(
        @JWTUser uuid: UUID,
        @QueryParam("page", "1") pageParam: Int,
        @QueryParam("limit", "9") limitParam: Int
    ): Response {
        val (page, limit, _) = pageableRoute(pageParam, limitParam, null, null)
        return Response.ok(memberFollowAnimeCacheService.getMissedAnimes(uuid, page, limit))
    }
}