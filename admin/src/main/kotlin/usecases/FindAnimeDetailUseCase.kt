package fr.shikkanime.admin.usecases

import fr.shikkanime.admin.dtos.anime.AdminAnimeDetailDto
import kotlin.uuid.Uuid

/**
 * Reads one stored anime with its metadata platform links for the administration API.
 *
 * The detail view joins the anime aggregate with its links to AniList, MyAnimeList and AniDB:
 * composing them here keeps the controller free of orchestration.
 */
interface FindAnimeDetailUseCase {
    /**
     * Reads the full administration view of one anime.
     *
     * @param id identifier of the anime to read.
     * @return the full anime view, or `null` when no anime carries this identifier.
     * @see [AdminAnimeDetailDto]
     */
    fun findById(id: Uuid): AdminAnimeDetailDto?
}
