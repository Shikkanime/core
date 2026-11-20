package fr.shikkanime.admin.usecases

import fr.shikkanime.admin.dtos.episode.AdminEpisodeDetailDto
import kotlin.uuid.Uuid

/**
 * Reads one stored episode with its metadata platform links for the administration API.
 *
 * The detail view joins the episode aggregate with its links to AniList, MyAnimeList and AniDB:
 * composing them here keeps the controller free of orchestration.
 */
interface FindEpisodeDetailUseCase {
    /**
     * Reads the full administration view of one episode.
     *
     * @param id identifier of the episode to read.
     * @return the full episode view, or `null` when no episode carries this identifier.
     * @see [AdminEpisodeDetailDto]
     */
    fun findById(id: Uuid): AdminEpisodeDetailDto?
}
