package fr.shikkanime.admin.controllers

import fr.shikkanime.admin.dtos.anime.AdminAnimeDetailDto
import fr.shikkanime.admin.dtos.anime.AdminAnimeSummaryDto
import fr.shikkanime.admin.dtos.anime.toAdminSummaryDto
import fr.shikkanime.admin.dtos.image.AdminImageDto
import fr.shikkanime.admin.dtos.requests.UpdateAnimeImageRequest
import fr.shikkanime.admin.usecases.AnimeImageUseCase
import fr.shikkanime.admin.usecases.FindAnimeDetailUseCase
import fr.shikkanime.database.services.AnimeService
import fr.shikkanime.ktor.*
import fr.shikkanime.ktor.dtos.ErrorMessageDto
import fr.shikkanime.ktor.dtos.MessageDto
import fr.shikkanime.ktor.pagination.PageDto
import fr.shikkanime.ktor.pagination.PageQuery
import fr.shikkanime.ktor.pagination.toDto
import fr.shikkanime.models.AnimeWithChildren
import fr.shikkanime.models.ImageType
import org.koin.core.annotation.Single
import kotlin.uuid.Uuid

/**
 * Administration read API on the stored anime catalog.
 */
@Suppress("unused")
@RestController("/v1/animes")
@Single(binds = [IController::class])
class AnimeController(
    private val animeService: AnimeService,
    private val findAnimeDetailUseCase: FindAnimeDetailUseCase,
    private val animeImageUseCase: AnimeImageUseCase
) : IController {
    /**
     * Lists one page of the stored animes.
     *
     * The search term is matched by the database - identity title, localized titles and identifier -
     * and the page is read with `limit`/`offset`: a filtered catalog costs one counted query and one
     * page query, never a full read. The response repeats the requested page, its size and the
     * total number of pages and elements, so the interface renders its paginator from one answer.
     *
     * @param query requested page and size, validated before the endpoint runs.
     * @param search optional term to filter the catalog with.
     * @return one page of compact anime rows, with its pagination bounds.
     * @see [AnimeService.findPage]
     */
    @GetMapping
    @Operation(
        "List stored animes",
        description = "Returns one page of the stored animes with their display title, platforms and " +
            "translation count, optionally filtered by a search term matched in the database.",
        tags = ["Animes"]
    )
    @ApiResponses([
        ApiResponse(200, "One page of stored animes", PageDto::class, [AdminAnimeSummaryDto::class])
    ])
    fun findAll(
        @Valid @ModelAttribute query: PageQuery,
        @QueryParam(name = "search", required = false) search: String?
    ): PageDto<AdminAnimeSummaryDto> =
        // Keep list and search on the same paged database query
        animeService.findPage(query.toPageRequest(), search)
            .toDto(AnimeWithChildren::toAdminSummaryDto)

    /**
     * Reads one stored anime with everything the database knows about it.
     *
     * @param id identifier of the anime to read.
     * @return the full anime view, or an error message when no anime carries this identifier.
     * @see [FindAnimeDetailUseCase]
     */
    @GetMapping("/{id}")
    @Operation(
        "Read a stored anime",
        description = "Returns the identity, audit columns, translations, platform links and artwork " +
            "of one stored anime.",
        tags = ["Animes"]
    )
    @ApiResponses([
        ApiResponse(200, "The stored anime", AdminAnimeDetailDto::class),
        ApiResponse(404, "No anime carries this identifier", ErrorMessageDto::class)
    ])
    fun findById(
        @PathParam("id") id: Uuid
    ): ResponseEntity<*> =
        findAnimeDetailUseCase.findById(id)
            ?.let(ResponseEntity.Companion::ok)
            ?: ResponseEntity.notFound(MessageDto.error("Anime not found"))

    /**
     * Overrides the artwork of a stored anime.
     *
     * The image is marked as manually managed: a platform harvest never replaces its URL
     * until the override is released through [resetImage].
     *
     * @param id identifier of the anime owning the artwork.
     * @param type type of the artwork to override.
     * @param request request carrying the URL of the replacement artwork.
     * @return the overridden image, or an error message when the anime is unknown or the
     * request is invalid.
     * @see [AnimeImageUseCase.update]
     */
    @PatchMapping("/{id}/images/{type}")
    @Operation(
        "Override an anime image",
        description = "Replaces the artwork of one type of a stored anime and marks it as " +
            "manually managed, so platform harvests keep it until the override is released.",
        tags = ["Animes"]
    )
    @ApiResponses([
        ApiResponse(200, "The overridden image", AdminImageDto::class),
        ApiResponse(400, "The image URL is invalid", ErrorMessageDto::class),
        ApiResponse(404, "No anime carries this identifier", ErrorMessageDto::class)
    ])
    fun updateImage(
        @PathParam("id") id: Uuid,
        @PathParam("type") type: ImageType,
        @RequestBody @Valid request: UpdateAnimeImageRequest
    ): ResponseEntity<*> =
        try {
            animeImageUseCase.update(id, type, request.url)
                ?.let(ResponseEntity.Companion::ok)
                ?: ResponseEntity.notFound(MessageDto.error("Anime not found"))
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest(MessageDto.error(e.message ?: "Invalid image URL"))
        }

    /**
     * Releases the manual management of the artwork of a stored anime.
     *
     * The current URL is kept until a platform harvest replaces it.
     *
     * @param id identifier of the anime owning the artwork.
     * @param type type of the artwork to release.
     * @return the released image, or an error message when the anime has no artwork of this
     * type.
     * @see [AnimeImageUseCase.reset]
     */
    @PostMapping("/{id}/images/{type}/reset")
    @Operation(
        "Release an anime image override",
        description = "Hands the artwork of one type of a stored anime back to the platform " +
            "harvests, keeping its current URL until the next one.",
        tags = ["Animes"]
    )
    @ApiResponses([
        ApiResponse(200, "The released image", AdminImageDto::class),
        ApiResponse(404, "The anime has no image of this type", ErrorMessageDto::class)
    ])
    fun resetImage(
        @PathParam("id") id: Uuid,
        @PathParam("type") type: ImageType
    ): ResponseEntity<*> =
        animeImageUseCase.reset(id, type)
            ?.let(ResponseEntity.Companion::ok)
            ?: ResponseEntity.notFound(MessageDto.error("Anime image not found"))
}
