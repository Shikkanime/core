package fr.shikkanime.controllers.admin.api

import com.google.inject.Inject
import fr.shikkanime.controllers.admin.ADMIN
import fr.shikkanime.dtos.PageableDto
import fr.shikkanime.factories.impl.AnimeFactory
import fr.shikkanime.factories.impl.EpisodeMappingFactory
import fr.shikkanime.factories.impl.TraceActionFactory
import fr.shikkanime.services.MemberFollowAnimeService
import fr.shikkanime.services.MemberFollowEpisodeService
import fr.shikkanime.services.MemberService
import fr.shikkanime.utils.routes.*
import fr.shikkanime.utils.routes.method.Get
import fr.shikkanime.utils.routes.param.PathParam
import fr.shikkanime.utils.routes.param.QueryParam
import java.time.LocalDate
import java.util.*

@Controller("$ADMIN/api/members")
class MemberController : HasPageableRoute() {
    @Inject private lateinit var memberService: MemberService
    @Inject private lateinit var memberFollowAnimeController: MemberFollowAnimeService
    @Inject private lateinit var memberFollowEpisodeService: MemberFollowEpisodeService
    @Inject private lateinit var animeFactory: AnimeFactory
    @Inject private lateinit var traceActionFactory: TraceActionFactory
    @Inject private lateinit var episodeMappingFactory: EpisodeMappingFactory

    @Path
    @Get
    @AdminSessionAuthenticated
    private fun getMembers(
        @QueryParam("page", "1") pageParam: Int,
        @QueryParam("limit", "9") limitParam: Int
    ): Response {
        val (page, limit, _) = pageableRoute(pageParam, limitParam, null, null)
        return Response.ok(memberService.findAllWithLastLogin(page, limit))
    }

    @Path("/{memberUuid}")
    @Get
    @AdminSessionAuthenticated
    private fun getMember(@PathParam memberUuid: UUID) =
        memberService.findDetailedMember(memberUuid)?.let { Response.ok(it) } ?: Response.notFound()

    @Path("/{memberUuid}/login-activities")
    @Get
    @AdminSessionAuthenticated
    private fun getMemberLoginActivities(@PathParam memberUuid: UUID): Response {
        val now = LocalDate.now()
        val after = now.minusMonths(1)
        val actions = memberService.findMemberLoginActivities(memberUuid, after)

        return Response.ok(
            after.datesUntil(now.plusDays(1))
                .toList()
                .associateWith { date -> actions.filter { traceAction -> traceAction.actionDateTime!!.toLocalDate().isEqual(date) }.map { traceActionFactory.toDto(it) } }
        )
    }

    @Path("/{memberUuid}/follow-anime-activities")
    @Get
    @AdminSessionAuthenticated
    private fun getMemberFollowAnimeActivities(@PathParam memberUuid: UUID): Response {
        val now = LocalDate.now()
        val after = now.minusMonths(1)
        val actions = memberService.findMemberFollowAnimeActivities(memberUuid, after)
        val total = memberFollowAnimeController.findAllFollowedAnimesUUID(memberUuid)

        return Response.ok(
            mapOf(
                "total" to total.size,
                "activities" to after.datesUntil(now.plusDays(1))
                    .toList()
                    .associateWith { date -> actions.filter { traceAction -> traceAction.actionDateTime!!.toLocalDate().isEqual(date) }.map { traceActionFactory.toDto(it) } }
            )
        )
    }

    @Path("/{memberUuid}/follow-episode-activities")
    @Get
    @AdminSessionAuthenticated
    private fun getMemberFollowEpisodeActivities(@PathParam memberUuid: UUID): Response {
        val now = LocalDate.now()
        val after = now.minusMonths(1)
        val actions = memberService.findMemberFollowEpisodeActivities(memberUuid, after)
        val total = memberFollowEpisodeService.findAllFollowedEpisodesUUID(memberUuid)

        return Response.ok(
            mapOf(
                "total" to total.size,
                "activities" to after.datesUntil(now.plusDays(1))
                    .toList()
                    .associateWith { date -> actions.filter { traceAction -> traceAction.actionDateTime!!.toLocalDate().isEqual(date) }.map { traceActionFactory.toDto(it) } }
            )
        )
    }

    @Path("/{memberUuid}/animes")
    @Get
    @AdminSessionAuthenticated
    private fun getMemberAnimes(
        @PathParam memberUuid: UUID,
        @QueryParam("page", "1") pageParam: Int,
        @QueryParam("limit", "9") limitParam: Int
    ): Response {
        val (page, limit, _) = pageableRoute(pageParam, limitParam, null, null)

        return Response.ok(
            PageableDto.fromPageable(
                memberFollowAnimeController.findAllFollowedAnimes(
                    memberService.find(memberUuid) ?: return Response.notFound(),
                    page,
                    limit,
                ),
                animeFactory
            )
        )
    }

    @Path("/{memberUuid}/episode-mappings")
    @Get
    @AdminSessionAuthenticated
    private fun getMemberEpisodeMappings(
        @PathParam memberUuid: UUID,
        @QueryParam("page", "1") pageParam: Int,
        @QueryParam("limit", "9") limitParam: Int
    ): Response {
        val (page, limit, _) = pageableRoute(pageParam, limitParam, null, null)

        return Response.ok(
            PageableDto.fromPageable(
                memberFollowEpisodeService.findAllFollowedEpisodes(
                    memberService.find(memberUuid) ?: return Response.notFound(),
                    page,
                    limit,
                ),
                episodeMappingFactory
            )
        )
    }
}