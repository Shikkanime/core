package fr.shikkanime.admin.controllers

import fr.shikkanime.admin.dtos.episode.AdminEpisodeDetailDto
import fr.shikkanime.admin.dtos.episode.AdminEpisodeSummaryDto
import fr.shikkanime.admin.dtos.episode.AdminLatestEpisodeDto
import fr.shikkanime.admin.dtos.episode.toAdminDetailDto
import fr.shikkanime.admin.dtos.episode.toAdminLatestDto
import fr.shikkanime.admin.dtos.episode.toAdminSummaryDto
import fr.shikkanime.database.services.AnimeService
import fr.shikkanime.database.services.EpisodeService
import fr.shikkanime.ktor.ApiResponse
import fr.shikkanime.ktor.ApiResponses
import fr.shikkanime.ktor.GetMapping
import fr.shikkanime.ktor.IController
import fr.shikkanime.ktor.ModelAttribute
import fr.shikkanime.ktor.Operation
import fr.shikkanime.ktor.PathParam
import fr.shikkanime.ktor.QueryParam
import fr.shikkanime.ktor.ResponseEntity
import fr.shikkanime.ktor.RestController
import fr.shikkanime.ktor.Valid
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
    private val animeService: AnimeService
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
        episodeService.findPageByAnimeId(animeId, query.toPageRequest())
            .toDto(EpisodeWithChildren::toAdminSummaryDto)

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
    ): PageDto<AdminLatestEpisodeDto> {
        val episodes = episodeService.findPageByReleaseDate(query.toPageRequest())
        val animes = animeService.findAllByIds(episodes.data.mapNotNull { it.episode.animeId }.distinct())

        return episodes.toDto { episode ->
            episode.toAdminLatestDto(animes[episode.episode.animeId]?.normalizedTitle.orEmpty())
        }
    }

    /**
     * Reads one stored episode with everything the database knows about it.
     *
     * @param id identifier of the episode to read.
     * @return the full episode view, or an error message when no episode carries this identifier.
     * @see [EpisodeService.findById]
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
    ): ResponseEntity<Any> =
        episodeService.findById(id)
            ?.let { ResponseEntity.ok(it.toAdminDetailDto()) }
            ?: ResponseEntity.notFound(MessageDto.error("Episode not found"))
}
