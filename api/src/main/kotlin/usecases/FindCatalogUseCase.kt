package fr.shikkanime.api.usecases

import fr.shikkanime.api.dtos.AnimeDto
import fr.shikkanime.api.dtos.EpisodeDto
import fr.shikkanime.core.pagination.PageRequest
import fr.shikkanime.ktor.pagination.PageDto
import kotlin.uuid.Uuid

/**
 * Read workflows of the public catalog, served from the framework cache when it is warm.
 *
 * Each read is cached under the bucket of its aggregate; a write path invalidates that bucket
 * after its transaction commits, so a cached page never outlives the data it was built from.
 *
 * @see fr.shikkanime.cache.Cache
 * @see fr.shikkanime.database.cache.CacheBuckets
 */
interface FindCatalogUseCase {
    /**
     * Reads one page of the anime catalog.
     *
     * @param request page and size to read.
     * @return the requested page of animes with its pagination bounds.
     * @see fr.shikkanime.database.services.AnimeService.findPage
     */
    suspend fun findAnimePage(request: PageRequest): PageDto<AnimeDto>

    /**
     * Reads one anime by its identifier.
     *
     * @param id identifier of the anime to read.
     * @return the anime view, or `null` when no anime carries this identifier.
     * @see fr.shikkanime.database.services.AnimeService.findById
     */
    suspend fun findAnime(id: Uuid): AnimeDto?

    /**
     * Reads one page of the episodes of an anime.
     *
     * @param animeId identifier of the anime owning the episodes.
     * @param request page and size to read.
     * @return the requested page of episodes with its pagination bounds.
     * @see fr.shikkanime.database.services.EpisodeService.findPageByAnimeId
     */
    suspend fun findEpisodePage(animeId: Uuid, request: PageRequest): PageDto<EpisodeDto>

    /**
     * Reads one episode by its identifier.
     *
     * @param id identifier of the episode to read.
     * @return the episode view, or `null` when no episode carries this identifier.
     * @see fr.shikkanime.database.services.EpisodeService.findById
     */
    suspend fun findEpisode(id: Uuid): EpisodeDto?
}
