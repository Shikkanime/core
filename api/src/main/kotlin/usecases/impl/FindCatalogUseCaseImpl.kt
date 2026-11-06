package fr.shikkanime.api.usecases.impl

import fr.shikkanime.api.dtos.AnimeDto
import fr.shikkanime.api.dtos.EpisodeDto
import fr.shikkanime.api.dtos.toDto
import fr.shikkanime.api.usecases.FindCatalogUseCase
import fr.shikkanime.cache.Cache
import fr.shikkanime.core.pagination.PageRequest
import fr.shikkanime.database.cache.CacheBuckets
import fr.shikkanime.database.services.AnimeService
import fr.shikkanime.database.services.EpisodeService
import fr.shikkanime.ktor.pagination.PageDto
import fr.shikkanime.ktor.pagination.toDto
import fr.shikkanime.models.AnimeWithChildren
import fr.shikkanime.models.EpisodeWithChildren
import org.koin.core.annotation.Single
import kotlin.time.Duration.Companion.minutes
import kotlin.uuid.Uuid

private val CACHE_TTL = 5.minutes

/**
 * Serves the catalog reads through the framework cache.
 *
 * A lookup answers from the local level first, then from Valkey, and only reaches the database on
 * a miss. The loader returns the API view, so a hit skips the SQL read and the whole aggregate
 * mapping; the bucket version changed by a write path makes every older entry unreachable.
 *
 * @see [Cache]
 * @see [CacheBuckets]
 */
@Suppress("unused")
@Single(binds = [FindCatalogUseCase::class])
internal class FindCatalogUseCaseImpl(
    private val animeService: AnimeService,
    private val episodeService: EpisodeService,
    private val cache: Cache
) : FindCatalogUseCase {
    override suspend fun findAnimePage(request: PageRequest): PageDto<AnimeDto> =
        cache.get(
            bucket = CacheBuckets.ANIME,
            key = pageKey(request.page, request.size),
            ttl = CACHE_TTL
        ) {
            animeService.findPage(request)
                .toDto(AnimeWithChildren::toDto)
        }

    override suspend fun findAnime(id: Uuid): AnimeDto? =
        cache.get<AnimeDto?>(
            bucket = CacheBuckets.ANIME,
            key = idKey(id),
            ttl = CACHE_TTL
        ) {
            animeService.findById(id)
                ?.toDto()
        }

    override suspend fun findEpisodePage(animeId: Uuid, request: PageRequest): PageDto<EpisodeDto> =
        cache.get(
            bucket = CacheBuckets.EPISODE,
            key = episodePageKey(animeId, request.page, request.size),
            ttl = CACHE_TTL
        ) {
            episodeService.findPageByAnimeId(animeId, request)
                .toDto(EpisodeWithChildren::toDto)
        }

    override suspend fun findEpisode(id: Uuid): EpisodeDto? =
        cache.get<EpisodeDto?>(
            bucket = CacheBuckets.EPISODE,
            key = idKey(id),
            ttl = CACHE_TTL
        ) {
            episodeService.findById(id)
                ?.toDto()
        }

    /**
     * Builds the cache key of one catalog page.
     *
     * @param page one-based index of the page.
     * @param size maximum number of rows the page holds.
     * @return the key identifying this page in the anime bucket.
     * @see [CacheBuckets.ANIME]
     */
    private fun pageKey(page: Int, size: Int): String =
        "page:$page:$size"

    /**
     * Builds the cache key of one aggregate read.
     *
     * @param id identifier of the aggregate.
     * @return the key identifying this aggregate in its bucket.
     */
    private fun idKey(id: Uuid): String =
        "id:$id"

    /**
     * Builds the cache key of one page of episodes.
     *
     * @param animeId identifier of the anime owning the episodes.
     * @param page one-based index of the page.
     * @param size maximum number of rows the page holds.
     * @return the key identifying this page in the episode bucket.
     * @see [CacheBuckets.EPISODE]
     */
    private fun episodePageKey(animeId: Uuid, page: Int, size: Int): String =
        "anime:$animeId:page:$page:$size"
}
