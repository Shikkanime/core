package fr.shikkanime.admin.controllers

import fr.shikkanime.admin.dtos.anime.AdminAnimeDetailDto
import fr.shikkanime.admin.dtos.anime.AdminAnimeSummaryDto
import fr.shikkanime.admin.dtos.anime.toAdminDetailDto
import fr.shikkanime.admin.dtos.anime.toAdminSummaryDto
import fr.shikkanime.database.services.AnimeService
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
import fr.shikkanime.models.AnimeWithChildren
import fr.shikkanime.models.Platform
import org.koin.core.annotation.Single
import kotlin.uuid.Uuid

/**
 * Administration read API on the stored anime catalog.
 */
@Suppress("unused")
@RestController("/v1/animes")
@Single(binds = [IController::class])
class AnimeController(
    private val animeService: AnimeService
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
     * @param platform optional streaming platform the animes must be linked to.
     * @return one page of compact anime rows, with its pagination bounds.
     * @see [AnimeService.findPage]
     * @see [AnimeService.findPageBySearch]
     * @see [AnimeService.findPageByPlatform]
     */
    @GetMapping
    @Operation(
        "List stored animes",
        description = "Returns one page of the stored animes with their display title, platforms and " +
            "translation count, optionally filtered by a search term or a streaming platform, both " +
            "matched in the database.",
        tags = ["Animes"]
    )
    @ApiResponses([
        ApiResponse(200, "One page of stored animes", PageDto::class, [AdminAnimeSummaryDto::class])
    ])
    fun findAll(
        @Valid @ModelAttribute query: PageQuery,
        @QueryParam(name = "search", required = false) search: String?,
        @QueryParam(name = "platform", required = false) platform: Platform?
    ): PageDto<AdminAnimeSummaryDto> {
        val request = query.toPageRequest()
        val animes = when {
            platform != null -> animeService.findPageByPlatform(platform, search, request)
            !search.isNullOrBlank() -> animeService.findPageBySearch(search, request)
            else -> animeService.findPage(request)
        }

        return animes.toDto(AnimeWithChildren::toAdminSummaryDto)
    }

    /**
     * Reads one stored anime with everything the database knows about it.
     *
     * @param id identifier of the anime to read.
     * @return the full anime view, or an error message when no anime carries this identifier.
     * @see [AnimeService.findById]
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
    ): ResponseEntity<Any> =
        animeService.findById(id)
            ?.let { ResponseEntity.ok(it.toAdminDetailDto()) }
            ?: ResponseEntity.notFound(MessageDto.error("Anime not found"))
}
