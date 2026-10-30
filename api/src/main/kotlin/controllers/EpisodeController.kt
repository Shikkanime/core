package fr.shikkanime.api.controllers

import fr.shikkanime.api.dtos.EpisodeDto
import fr.shikkanime.api.dtos.toDto
import fr.shikkanime.database.services.EpisodeService
import fr.shikkanime.models.EpisodeWithChildren
import fr.shikkanime.ktor.ApiResponse
import fr.shikkanime.ktor.ApiResponses
import fr.shikkanime.ktor.GetMapping
import fr.shikkanime.ktor.IController
import fr.shikkanime.ktor.Operation
import fr.shikkanime.ktor.PathParam
import fr.shikkanime.ktor.QueryParam
import fr.shikkanime.ktor.ResponseEntity
import fr.shikkanime.ktor.RestController
import fr.shikkanime.ktor.dtos.ErrorMessageDto
import fr.shikkanime.ktor.dtos.MessageDto
import org.koin.core.annotation.Single
import kotlin.uuid.Uuid

/**
 * Read-only REST API on the stored episode aggregates.
 */
@Suppress("unused")
@RestController("/v1/episodes")
@Single(binds = [IController::class])
class EpisodeController(
    private val episodeService: EpisodeService
) : IController {
    /**
     * Lists every episode of an anime with its children.
     *
     * @param animeId identifier of the anime owning the episodes.
     * @return the episodes of the anime as DTOs, in broadcast order.
     * @see [EpisodeService.findAllByAnimeId]
     */
    @GetMapping
    @Operation(
        "List the episodes of an anime",
        description = "Returns every episode of an anime, with its localizations, releases and artwork.",
        tags = ["Episodes"]
    )
    @ApiResponses([
        ApiResponse(200, "The episodes of the anime", List::class, [EpisodeDto::class])
    ])
    fun findAllByAnimeId(@QueryParam("animeId") animeId: Uuid): List<EpisodeDto> =
        // The episodes of the anime are asked in one read, the mapping is only a shape change
        episodeService.findAllByAnimeId(animeId)
            .map(EpisodeWithChildren::toDto)

    /**
     * Loads one episode by its identifier.
     *
     * @param id identifier of the episode to load.
     * @return the episode with its children, or a 404 when it does not exist.
     * @see [EpisodeService.findById]
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
    fun findById(@PathParam("id") id: Uuid): ResponseEntity<*> =
        // Load the stored aggregate of the requested identifier
        episodeService.findById(id)
            ?.let { ResponseEntity.ok(it.toDto()) }
            ?: ResponseEntity.notFound(MessageDto.error("No episode with identifier $id"))
}
