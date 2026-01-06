package fr.shikkanime.controllers.admin.api

import com.google.inject.Inject
import fr.shikkanime.controllers.admin.ADMIN
import fr.shikkanime.dtos.PageableDto
import fr.shikkanime.dtos.analytics.KeyCountDto
import fr.shikkanime.factories.impl.AnimeFactory
import fr.shikkanime.factories.impl.EpisodeMappingFactory
import fr.shikkanime.services.MemberFollowAnimeService
import fr.shikkanime.services.MemberFollowEpisodeService
import fr.shikkanime.services.MemberService
import fr.shikkanime.utils.routes.*
import fr.shikkanime.utils.routes.method.Get
import fr.shikkanime.utils.routes.param.PathParam
import fr.shikkanime.utils.routes.param.QueryParam
import java.time.LocalDate
import java.util.*
import java.util.stream.Stream

@Controller("$ADMIN/api/members")
@AdminSessionAuthenticated
class MemberController : HasPageableRoute() {
    @Inject private lateinit var memberService: MemberService
    @Inject
    private lateinit var memberFollowAnimeService: MemberFollowAnimeService
    @Inject private lateinit var memberFollowEpisodeService: MemberFollowEpisodeService
    @Inject private lateinit var animeFactory: AnimeFactory
    @Inject private lateinit var episodeMappingFactory: EpisodeMappingFactory

    @Path
    @Get
    private fun getMembers(
        @QueryParam("page", "1") pageParam: Int,
        @QueryParam("limit", "9") limitParam: Int
    ): Response {
        val (page, limit, _) = pageableRoute(pageParam, limitParam, null, null)
        return Response.ok(memberService.findAllWithLastLogin(page, limit))
    }

    @Path("/{memberUuid}")
    @Get
    private fun getMember(@PathParam memberUuid: UUID) =
        memberService.findDetailedMember(memberUuid)?.let(Response::ok) ?: Response.notFound()

    @Path("/{memberUuid}/login-activities")
    @Get
    private fun getMemberLoginActivities(@PathParam memberUuid: UUID): Response {
        val now = LocalDate.now()
        val dateRange = now.minusMonths(1).datesUntil(now.plusDays(1))
        val actions = memberService.getMemberLoginCounts(memberUuid).associateBy { it.key }
        return Response.ok(dateRange.toList().map { date -> actions[date.toString()] ?: KeyCountDto(date.toString(), 0) })
    }

    @Path("/{memberUuid}/follow-anime-activities")
    @Get
    private fun getMemberFollowAnimeActivities(@PathParam memberUuid: UUID): Response {
        val now = LocalDate.now()
        val dateRange = now.minusMonths(1).datesUntil(now.plusDays(1))
        val actions = memberService.getCumulativeMemberFollowAnimeCounts(memberUuid)
        return Response.ok(aggregateKeyCounts(actions, dateRange))
    }

    private fun aggregateKeyCounts(
        actions: List<KeyCountDto>,
        dateRange: Stream<LocalDate>
    ): MutableList<KeyCountDto> {
        val result = mutableListOf<KeyCountDto>()
        var lastCount = 0L
        var actionIndex = 0
        // Pre-parse dates to avoid parsing in the loop
        val parsedActions = actions.map { it to LocalDate.parse(it.key) }

        for (date in dateRange) {
            while (actionIndex < parsedActions.size && parsedActions[actionIndex].second <= date) {
                lastCount = parsedActions[actionIndex].first.count
                actionIndex++
            }
            result.add(KeyCountDto(date.toString(), lastCount))
        }

        return result
    }

    @Path("/{memberUuid}/follow-episode-activities")
    @Get
    private fun getMemberFollowEpisodeActivities(@PathParam memberUuid: UUID): Response {
        val now = LocalDate.now()
        val dateRange = now.minusMonths(1).datesUntil(now.plusDays(1))
        val actions = memberService.getCumulativeMemberFollowEpisodeCounts(memberUuid)
        return Response.ok(aggregateKeyCounts(actions, dateRange))
    }

    @Path("/{memberUuid}/animes")
    @Get
    private fun getMemberAnimes(
        @PathParam memberUuid: UUID,
        @QueryParam("page", "1") pageParam: Int,
        @QueryParam("limit", "9") limitParam: Int
    ): Response {
        val (page, limit, _) = pageableRoute(pageParam, limitParam, null, null)

        return Response.ok(
            PageableDto.fromPageable(
                memberFollowAnimeService.findAllFollowedAnimes(
                    memberUuid,
                    page,
                    limit,
                ),
                animeFactory
            )
        )
    }

    @Path("/{memberUuid}/episode-mappings")
    @Get
    private fun getMemberEpisodeMappings(
        @PathParam memberUuid: UUID,
        @QueryParam("page", "1") pageParam: Int,
        @QueryParam("limit", "9") limitParam: Int
    ): Response {
        val (page, limit, _) = pageableRoute(pageParam, limitParam, null, null)

        return Response.ok(
            PageableDto.fromPageable(
                memberFollowEpisodeService.findAllFollowedEpisodes(
                    memberUuid,
                    page,
                    limit,
                ),
                episodeMappingFactory
            )
        )
    }
}