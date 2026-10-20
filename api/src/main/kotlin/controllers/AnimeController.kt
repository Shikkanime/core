package fr.shikkanime.api.controllers

import fr.shikkanime.api.dtos.AnimeDto
import fr.shikkanime.api.dtos.toDto
import fr.shikkanime.database.services.AnimeService
import fr.shikkanime.models.AnimeWithChildren
import fr.shikkanime.ktor.ApiResponse
import fr.shikkanime.ktor.ApiResponses
import fr.shikkanime.ktor.GetMapping
import fr.shikkanime.ktor.IController
import fr.shikkanime.ktor.Operation
import fr.shikkanime.ktor.PathParam
import fr.shikkanime.ktor.ResponseEntity
import fr.shikkanime.ktor.RestController
import fr.shikkanime.ktor.dtos.ErrorMessageDto
import fr.shikkanime.ktor.dtos.MessageDto
import org.koin.core.annotation.Single
import kotlin.uuid.Uuid

/**
 * Read-only REST API on the stored anime aggregates.
 */
@Suppress("unused")
@RestController("/v1/animes")
@Single(binds = [IController::class])
class AnimeController(
    private val animeService: AnimeService
) : IController {
    /**
     * Lists every stored anime with its children.
     *
     * @return the anime catalog as DTOs.
     * @see [AnimeService.findAll]
     */
    @GetMapping
    @Operation(
        "List stored animes",
        description = "Returns every stored anime, with its localizations, sources and artwork.",
        tags = ["Animes"]
    )
    @ApiResponses([
        ApiResponse(200, "The stored animes", List::class, [AnimeDto::class])
    ])
    fun findAll(): List<AnimeDto> =
        // The whole catalog is asked in one read, the mapping is only a shape change
        animeService.findAll()
            .map(AnimeWithChildren::toDto)

    /**
     * Loads one anime by its identifier.
     *
     * @param id identifier of the anime to load.
     * @return the anime with its children, or a 404 when it does not exist.
     * @see [AnimeService.findById]
     */
    @GetMapping("/{id}")
    @Operation(
        "Get a stored anime",
        description = "Returns one stored anime, with its localizations, sources and artwork.",
        tags = ["Animes"]
    )
    @ApiResponses([
        ApiResponse(200, "The stored anime", AnimeDto::class),
        ApiResponse(404, "No anime with this identifier", ErrorMessageDto::class)
    ])
    fun findById(@PathParam("id") id: Uuid): ResponseEntity<*> =
        // Load the stored aggregate of the requested identifier
        animeService.findById(id)
            ?.let { ResponseEntity.ok(it.toDto()) }
            ?: ResponseEntity.notFound(MessageDto.error("No anime with identifier $id"))
}
