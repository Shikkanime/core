package fr.shikkanime.controllers.admin.api

import com.google.inject.Inject
import fr.shikkanime.controllers.admin.ADMIN
import fr.shikkanime.converters.AbstractConverter
import fr.shikkanime.dtos.PageableDto
import fr.shikkanime.dtos.TraceActionDto
import fr.shikkanime.dtos.animes.AnimeDto
import fr.shikkanime.dtos.mappings.EpisodeMappingDto
import fr.shikkanime.services.MemberFollowAnimeService
import fr.shikkanime.services.MemberFollowEpisodeService
import fr.shikkanime.services.MemberService
import fr.shikkanime.utils.routes.*
import fr.shikkanime.utils.routes.method.Get
import fr.shikkanime.utils.routes.openapi.OpenAPI
import fr.shikkanime.utils.routes.param.PathParam
import fr.shikkanime.utils.routes.param.QueryParam
import java.time.LocalDate
import java.util.*

@Controller("$ADMIN/api/members")
class MemberController : HasPageableRoute() {
    @Inject
    private lateinit var memberService: MemberService

    @Inject
    private lateinit var memberFollowAnimeController: MemberFollowAnimeService

    @Inject
    private lateinit var memberFollowEpisodeService: MemberFollowEpisodeService

    @Path
    @Get
    @AdminSessionAuthenticated
    @OpenAPI(hidden = true)
    private fun getMembers(
        @QueryParam("page", description = "Page number for pagination")
        pageParam: Int?,
        @QueryParam("limit", description = "Number of items per page. Must be between 1 and 30", example = "15")
        limitParam: Int?,
    ): Response {
        val (page, limit, _) = pageableRoute(pageParam, limitParam, null, null)
        return Response.ok(memberService.findAllWithLastLogin(page, limit))
    }

    @Path("/{memberUuid}")
    @Get
    @AdminSessionAuthenticated
    @OpenAPI(hidden = true)
    private fun getMember(
        @PathParam("memberUuid", description = "Member UUID")
        memberUuid: UUID
    ): Response {
        return Response.ok(memberService.findDetailedMember(memberUuid) ?: return Response.notFound())
    }

    @Path("/{memberUuid}/login-activities")
    @Get
    @AdminSessionAuthenticated
    @OpenAPI(hidden = true)
    private fun getMemberLoginActivities(
        @PathParam("memberUuid", description = "Member UUID")
        memberUuid: UUID
    ): Response {
        val now = LocalDate.now()
        val after = now.minusMonths(1)
        val actions = memberService.findMemberLoginActivities(memberUuid, after)

        return Response.ok(
            after.datesUntil(now.plusDays(1))
                .toList()
                .associateWith { date -> AbstractConverter.convert(actions.filter { traceAction -> traceAction.actionDateTime!!.toLocalDate() == date }, TraceActionDto::class.java) }
        )
    }

    @Path("/{memberUuid}/follow-anime-activities")
    @Get
    @AdminSessionAuthenticated
    @OpenAPI(hidden = true)
    private fun getMemberFollowAnimeActivities(
        @PathParam("memberUuid", description = "Member UUID")
        memberUuid: UUID
    ): Response {
        val now = LocalDate.now()
        val after = now.minusMonths(1)
        val actions = memberService.findMemberFollowAnimeActivities(memberUuid, after)

        return Response.ok(
            after.datesUntil(now.plusDays(1))
                .toList()
                .associateWith { date -> AbstractConverter.convert(actions.filter { traceAction -> traceAction.actionDateTime!!.toLocalDate() == date }, TraceActionDto::class.java) }
        )
    }

    @Path("/{memberUuid}/follow-episode-activities")
    @Get
    @AdminSessionAuthenticated
    @OpenAPI(hidden = true)
    private fun getMemberFollowEpisodeActivities(
        @PathParam("memberUuid", description = "Member UUID")
        memberUuid: UUID
    ): Response {
        val now = LocalDate.now()
        val after = now.minusMonths(1)
        val actions = memberService.findMemberFollowEpisodeActivities(memberUuid, after)

        return Response.ok(
            after.datesUntil(now.plusDays(1))
                .toList()
                .associateWith { date -> AbstractConverter.convert(actions.filter { traceAction -> traceAction.actionDateTime!!.toLocalDate() == date }, TraceActionDto::class.java) }
        )
    }

    @Path("/{memberUuid}/animes")
    @Get
    @AdminSessionAuthenticated
    @OpenAPI(hidden = true)
    private fun getMemberAnimes(
        @PathParam("memberUuid", description = "Member UUID")
        memberUuid: UUID,
        @QueryParam("page", description = "Page number for pagination")
        pageParam: Int?,
        @QueryParam("limit", description = "Number of items per page. Must be between 1 and 30", example = "15")
        limitParam: Int?,
    ): Response {
        val (page, limit, _) = pageableRoute(pageParam, limitParam, null, null)

        return Response.ok(
            PageableDto.fromPageable(
                memberFollowAnimeController.findAllFollowedAnimes(
                    memberService.find(memberUuid) ?: return Response.notFound(),
                    page,
                    limit,
                ),
                AnimeDto::class.java
            )
        )
    }

    @Path("/{memberUuid}/episode-mappings")
    @Get
    @AdminSessionAuthenticated
    @OpenAPI(hidden = true)
    private fun getMemberEpisodeMappings(
        @PathParam("memberUuid", description = "Member UUID")
        memberUuid: UUID,
        @QueryParam("page", description = "Page number for pagination")
        pageParam: Int?,
        @QueryParam("limit", description = "Number of items per page. Must be between 1 and 30", example = "15")
        limitParam: Int?,
    ): Response {
        val (page, limit, _) = pageableRoute(pageParam, limitParam, null, null)

        return Response.ok(
            PageableDto.fromPageable(
                memberFollowEpisodeService.findAllFollowedEpisodes(
                    memberService.find(memberUuid) ?: return Response.notFound(),
                    page,
                    limit,
                ),
                EpisodeMappingDto::class.java
            )
        )
    }
}