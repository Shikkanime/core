package fr.shikkanime.admin.usecases

import fr.shikkanime.admin.dtos.episode.AdminLatestEpisodeDto
import fr.shikkanime.core.pagination.PageRequest
import fr.shikkanime.ktor.pagination.PageDto

/**
 * Lists the stored episodes, newest release first, for the administration API.
 *
 * Each row is joined with the normalized title of its anime: composing the page here keeps the
 * controller free of the second lookup.
 */
interface FindLatestEpisodesUseCase {
    /**
     * Reads one page of episodes ordered by release date.
     *
     * @param request validated page bounds.
     * @return one page of episode rows with their anime title, with its pagination bounds.
     * @see [AdminLatestEpisodeDto]
     */
    fun findLatest(request: PageRequest): PageDto<AdminLatestEpisodeDto>
}
