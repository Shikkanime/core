package fr.shikkanime.admin.controllers

import fr.shikkanime.admin.dtos.episode.AdminEpisodeDetailDto
import fr.shikkanime.admin.dtos.episode.AdminEpisodeSummaryDto
import fr.shikkanime.admin.dtos.episode.AdminLatestEpisodeDto
import fr.shikkanime.admin.dtos.episode.toAdminSummaryDto
import fr.shikkanime.admin.usecases.FindEpisodeDetailUseCase
import fr.shikkanime.admin.usecases.FindLatestEpisodesUseCase
import fr.shikkanime.database.services.EpisodeService
import fr.shikkanime.ktor.*
import fr.shikkanime.ktor.dtos.ErrorMessageDto
import fr.shikkanime.ktor.dtos.MessageDto
import fr.shikkanime.ktor.pagination.PageDto
import fr.shikkanime.ktor.pagination.PageQuery
import fr.shikkanime.ktor.pagination.toDto
import fr.shikkanime.models.EpisodeWithChildren
import org.koin.core.annotation.Single
import kotlin.uuid.Uuid

/**
 * Administration read API on the episodes of a stored anime.
 */
@Suppress("unused")
@RestController("/v1/episodes")
@Single(binds = [IController::class])
class EpisodeController(
    private val episodeService: EpisodeService,
    private val findEpisodeDetailUseCase: FindEpisodeDetailUseCase,
    private val findLatestEpisodesUseCase: FindLatestEpisodesUseCase
) : IController {
    /**
     * Lists one page of the episodes of an anime.
     *
     * @param animeId identifier of the anime owning the episodes.
     * @param query requested page and size, validated before the endpoint runs.
     * @return one page of compact episode rows, with its pagination bounds.
     * @see [EpisodeService.findPageByAnimeId]
     */
    @GetMapping
    @Operation(
        "List the episodes of an anime",
        description = "Returns one page of the episodes of an anime with their identity, broadcast " +
            "date, duration, display title and release count.",
        tags = ["Episodes"]
    )
    @ApiResponses([
        ApiResponse(200, "One page of episodes", PageDto::class, [AdminEpisodeSummaryDto::class])
    ])
    fun findAllByAnimeId(
        @QueryParam("animeId") animeId: Uuid,
        @Valid @ModelAttribute query: PageQuery
    ): PageDto<AdminEpisodeSummaryDto> =
        // Read only the requested page of the anime episodes, never its whole catalog
        episodeService.findPageByAnimeId(animeId, query.toPageRequest())
            .toDto(EpisodeWithChildren::toAdminSummaryDto)

    /**
     * Lists one page of the stored episodes, newest release first.
     *
     * @param query requested page and size, validated before the endpoint runs.
     * @return one page of episodes with their anime title, with its pagination bounds.
     * @see [FindLatestEpisodesUseCase]
     */
    @GetMapping("/latest")
    @Operation(
        "List every episode, newest release first",
        description = "Returns one page of the stored episodes ordered by their own release date, " +
            "newest first, with the normalized title of their anime. The date of their platform " +
            "releases does not order the list.",
        tags = ["Episodes"]
    )
    @ApiResponses([
        ApiResponse(200, "One page of episodes", PageDto::class, [AdminLatestEpisodeDto::class])
    ])
    fun findLatest(
        @Valid @ModelAttribute query: PageQuery
    ): PageDto<AdminLatestEpisodeDto> =
        findLatestEpisodesUseCase.findLatest(query.toPageRequest())

    /**
     * Reads one stored episode with everything the database knows about it.
     *
     * @param id identifier of the episode to read.
     * @return the full episode view, or an error message when no episode carries this identifier.
     * @see [FindEpisodeDetailUseCase]
     */
    @GetMapping("/{id}")
    @Operation(
        "Read a stored episode",
        description = "Returns the identity, audit columns, translations, platform releases and " +
            "artwork of one stored episode.",
        tags = ["Episodes"]
    )
    @ApiResponses([
        ApiResponse(200, "The stored episode", AdminEpisodeDetailDto::class),
        ApiResponse(404, "No episode carries this identifier", ErrorMessageDto::class)
    ])
    fun findById(
        @PathParam("id") id: Uuid
    ): ResponseEntity<*> =
        findEpisodeDetailUseCase.findById(id)
            ?.let(ResponseEntity.Companion::ok)
            ?: ResponseEntity.notFound(MessageDto.error("Episode not found"))
}
