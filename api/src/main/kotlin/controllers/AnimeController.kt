package fr.shikkanime.api.controllers

import fr.shikkanime.api.dtos.AnimeDto
import fr.shikkanime.api.usecases.FindCatalogUseCase
import fr.shikkanime.ktor.ApiResponse
import fr.shikkanime.ktor.ApiResponses
import fr.shikkanime.ktor.GetMapping
import fr.shikkanime.ktor.IController
import fr.shikkanime.ktor.ModelAttribute
import fr.shikkanime.ktor.Operation
import fr.shikkanime.ktor.PathParam
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
 * Read-only REST API on the stored anime aggregates.
 */
@Suppress("unused")
@RestController("/v1/animes")
@Single(binds = [IController::class])
class AnimeController(
    private val findCatalogUseCase: FindCatalogUseCase
) : IController {
    /**
     * Lists one page of the stored animes.
     *
     * @param query requested page and size, validated before the endpoint runs.
     * @return one page of anime views with its pagination bounds.
     * @see [FindCatalogUseCase.findAnimePage]
     */
    @GetMapping
    @Operation(
        "List stored animes",
        description = "Returns one page of the stored animes, with their localizations, sources and artwork.",
        tags = ["Animes"]
    )
    @ApiResponses([
        ApiResponse(200, "One page of stored animes", PageDto::class, [AnimeDto::class])
    ])
    suspend fun findAll(
        @Valid @ModelAttribute query: PageQuery
    ): PageDto<AnimeDto> =
        findCatalogUseCase.findAnimePage(query.toPageRequest())

    /**
     * Loads one anime by its identifier.
     *
     * @param id identifier of the anime to load.
     * @return the anime with its children, or a 404 when it does not exist.
     * @see [FindCatalogUseCase.findAnime]
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
    suspend fun findById(@PathParam("id") id: Uuid): ResponseEntity<*> =
        findCatalogUseCase.findAnime(id)
            ?.let { ResponseEntity.ok(it) }
            ?: ResponseEntity.notFound(MessageDto.error("No anime with identifier $id"))
}
