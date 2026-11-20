package fr.shikkanime.api.controllers

import fr.shikkanime.api.dtos.episodegroup.EpisodeGroupDto
import fr.shikkanime.api.dtos.episodegroup.toDto
import fr.shikkanime.database.services.EpisodeGroupService
import fr.shikkanime.ktor.*
import fr.shikkanime.ktor.dtos.ErrorMessageDto
import fr.shikkanime.ktor.pagination.PageDto
import fr.shikkanime.ktor.pagination.PageQuery
import fr.shikkanime.ktor.pagination.toDto
import fr.shikkanime.models.Country
import fr.shikkanime.models.EpisodeType
import fr.shikkanime.models.LangType
import kotlinx.datetime.LocalDateTime
import org.koin.core.annotation.Single
import kotlin.uuid.Uuid

/**
 * Read-only REST API listing episode groups, computed from the stored releases.
 */
@Suppress("unused")
@RestController("/v1/episode-groups")
@Single(binds = [IController::class])
class EpisodeGroupController(
    private val episodeGroupService: EpisodeGroupService
) : IController {

    /**
     * Lists one page of the most recently released episode groups.
     *
     * Groups are computed on every call from the releases stored in the window,
     * so the answer never depends on an index rebuilt at startup. Without
     * `from`/`to` the window covers the last 30 days; `to` left empty keeps
     * future-dated releases.
     *
     * @param country market the locale visibility and lang types resolve for.
     * @param from earliest release date included (ISO local date-time), or none for the default window.
     * @param to latest release date included (ISO local date-time), or none for no upper bound.
     * @param animeId optional anime filter.
     * @param episodeType optional episode type filter.
     * @param searchTypes optional comma-separated lang types (VOICE,SUBTITLES).
     * @param query requested page and size, validated before the endpoint runs.
     * @return one page of groups with their episodes and releases, newest first.
     * @see [EpisodeGroupService.findLatest]
     */
    @GetMapping
    @Operation(
        "List the latest episode groups",
        description = "Returns one page of episode groups released together, most recent first, " +
            "for the requested market. Each group carries its episode coverage per lang type " +
            "and the platform releases of its members.",
        tags = ["Episode groups"]
    )
    @ApiResponses([
        ApiResponse(200, "One page of episode groups", PageDto::class, [EpisodeGroupDto::class])
    ])
    fun findLatest(
        @QueryParam("country") country: Country = Country.FR,
        @QueryParam("from") from: String? = null,
        @QueryParam("to") to: String? = null,
        @QueryParam("animeId") animeId: Uuid? = null,
        @QueryParam("episodeType") episodeType: EpisodeType? = null,
        @QueryParam("searchTypes") searchTypes: String? = null,
        @Valid @ModelAttribute query: PageQuery
    ): PageDto<EpisodeGroupDto> =
        episodeGroupService.findLatest(
            country = country,
            from = from?.let(LocalDateTime::parse),
            to = to?.let(LocalDateTime::parse),
            animeId = animeId,
            episodeType = episodeType,
            searchTypes = searchTypes?.split(',')
                ?.map { it.trim() }
                ?.filter { it.isNotEmpty() }
                ?.map(LangType::valueOf)
                ?.toSet(),
            request = query.toPageRequest()
        ).toDto { it.toDto(country) }
}
