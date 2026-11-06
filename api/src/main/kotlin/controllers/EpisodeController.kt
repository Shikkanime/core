package fr.shikkanime.api.controllers

import fr.shikkanime.api.dtos.EpisodeDto
import fr.shikkanime.api.usecases.FindCatalogUseCase
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
import org.koin.core.annotation.Single
import kotlin.uuid.Uuid

/**
 * Read-only REST API on the stored episode aggregates.
 */
@Suppress("unused")
@RestController("/v1/episodes")
@Single(binds = [IController::class])
class EpisodeController(
    private val findCatalogUseCase: FindCatalogUseCase
) : IController {
    /**
     * Lists one page of the episodes of an anime.
     *
     * @param animeId identifier of the anime owning the episodes.
     * @param query requested page and size, validated before the endpoint runs.
     * @return one page of episode views with its pagination bounds.
     * @see [FindCatalogUseCase.findEpisodePage]
     */
    @GetMapping
    @Operation(
        "List the episodes of an anime",
        description = "Returns one page of the episodes of an anime, with their localizations, releases " +
            "and artwork.",
        tags = ["Episodes"]
    )
    @ApiResponses([
        ApiResponse(200, "One page of the episodes of the anime", PageDto::class, [EpisodeDto::class])
    ])
    suspend fun findAllByAnimeId(
        @QueryParam("animeId") animeId: Uuid,
        @Valid @ModelAttribute query: PageQuery
    ): PageDto<EpisodeDto> =
        findCatalogUseCase.findEpisodePage(animeId, query.toPageRequest())

    /**
     * Loads one episode by its identifier.
     *
     * @param id identifier of the episode to load.
     * @return the episode with its children, or a 404 when it does not exist.
     * @see [FindCatalogUseCase.findEpisode]
     */
    @GetMapping("/{id}")
    @Operation(
        "Get a stored episode",
        description = "Returns one stored episode, with its localizations, releases and artwork.",
        tags = ["Episodes"]
    )
    @ApiResponses([
        ApiResponse(200, "The stored episode", EpisodeDto::class),
        ApiResponse(404, "No episode with this identifier", ErrorMessageDto::class)
    ])
    suspend fun findById(@PathParam("id") id: Uuid): ResponseEntity<*> =
        findCatalogUseCase.findEpisode(id)
            ?.let { ResponseEntity.ok(it) }
            ?: ResponseEntity.notFound(MessageDto.error("No episode with identifier $id"))
}
