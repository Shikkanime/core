package fr.shikkanime.api.controllers

import fr.shikkanime.api.dtos.AnimeDto
import fr.shikkanime.api.dtos.AnimeDto.Companion.toDto
import fr.shikkanime.database.services.AnimeService
import fr.shikkanime.ktor.ApiResponse
import fr.shikkanime.ktor.ApiResponses
import fr.shikkanime.ktor.GetMapping
import fr.shikkanime.ktor.IController
import fr.shikkanime.ktor.Operation
import fr.shikkanime.ktor.PathParam
import fr.shikkanime.ktor.ResponseEntity
import fr.shikkanime.ktor.RestController
import org.koin.core.annotation.Single
import kotlin.uuid.Uuid

/**
 * Read-only REST API on the stored anime aggregates.
 */
@Suppress("unused")
@RestController("/animes")
@Single(binds = [IController::class])
class AnimeController(
    private val animeService: AnimeService
) : IController {
    @GetMapping
    @Operation(
        "List stored animes",
        description = "Returns every stored anime, with its localizations, sources and artwork.",
        tags = ["Animes"]
    )
    @ApiResponses([
        ApiResponse(200, "The stored animes", List::class, [AnimeDto::class])
    ])
    fun findAll(): ResponseEntity<*> =
        ResponseEntity.ok(animeService.findAll().map(::toDto))

    @GetMapping("/{id}")
    @Operation(
        "Get a stored anime",
        description = "Returns one stored anime, with its localizations, sources and artwork.",
        tags = ["Animes"]
    )
    @ApiResponses([
        ApiResponse(200, "The stored anime", AnimeDto::class),
        ApiResponse(404, "No anime with this identifier", String::class)
    ])
    fun findById(@PathParam("id") id: String): ResponseEntity<*> =
        animeService.findById(Uuid.parse(id))
            ?.let { ResponseEntity.ok(toDto(it)) }
            ?: ResponseEntity.notFound("No anime with identifier $id")
}
