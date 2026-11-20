package fr.shikkanime.jobs.metadata

import fr.shikkanime.core.LoggerFactory
import fr.shikkanime.jobs.metadata.MyAnimeListClient.Companion.BASE_URL
import fr.shikkanime.jobs.metadata.MyAnimeListClient.Companion.RATE_LIMIT_PER_MINUTE
import fr.shikkanime.ktor.createHttpClient
import fr.shikkanime.models.MetadataPlatform
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.delay
import org.koin.core.annotation.Single
import java.time.LocalDate
import java.time.ZonedDateTime
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.seconds

/**
 * Client of the MyAnimeList API through the tenrai mirror, as prototyped in the shikkanime
 * scraper.
 *
 * Every call goes through the shared [MetadataApiCache] and the [RateLimiter]: the platform
 * bans aggressive clients, so a request is only ever sent once per cache window and at most
 * [RATE_LIMIT_PER_MINUTE] times per minute.
 */
@Single
class MyAnimeListClient(
    private val cache: MetadataApiCache,
    private val rateLimiter: RateLimiter = RateLimiter(RATE_LIMIT_PER_MINUTE)
) {
    private val logger = LoggerFactory.getLogger()
    private val httpClient = createHttpClient()

    /**
     * Searches anime entries by title.
     *
     * @param query free-text search, matched by the platform against every known title.
     * @return the matching entries, best match first.
     */
    suspend fun searchAnime(query: String): List<ExternalAnime> {
        val body = cache.getOrPut(
            cache.key(MetadataPlatform.MYANIMELIST, "search", query),
            CACHE_TTL
        ) {
            get("/v1/anime", mapOf("q" to query, "limit" to SEARCH_LIMIT.toString()))
        }
        return cache.decode<MyAnimeListSearchResponse>(body).data.map { it.toExternalAnime() }
    }

    /**
     * Loads one anime entry with its editorial data.
     *
     * @param id MyAnimeList identifier of the entry.
     * @return the matching entry, or `null` when the platform does not know it.
     */
    suspend fun getAnimeById(id: Int): ExternalAnime? {
        val body = cache.getOrPut(
            cache.key(MetadataPlatform.MYANIMELIST, "anime", id.toString()),
            CACHE_TTL
        ) {
            get("/v1/anime/$id/full")
        }
        return cache.decode<MyAnimeListMediaResponse>(body).data?.toExternalAnime()
    }

    /**
     * Loads the episode entries of one anime.
     *
     * The endpoint is paginated: every page is fetched and cached under its own key, so a
     * long-running series costs one request per page and never more.
     *
     * @param id MyAnimeList identifier of the anime owning the episodes.
     * @return the episode entries in platform order.
     */
    suspend fun getEpisodesByMediaId(id: Int): List<ExternalEpisode> {
        val episodes = mutableListOf<ExternalEpisode>()
        var page = 1
        var hasNextPage: Boolean

        do {
            val body = cache.getOrPut(
                cache.key(MetadataPlatform.MYANIMELIST, "episodes", id.toString(), "page$page"),
                CACHE_TTL
            ) {
                get("/v1/anime/$id/episodes?page=$page")
            }
            val response = cache.decode<MyAnimeListEpisodeResponse>(body)
            hasNextPage = response.pagination.hasNextPage
            page++

            // mal_id is only unique inside its anime: the composite keeps the episode
            // identifier globally meaningful.
            episodes.addAll(
                response.data.mapIndexed { index, episode ->
                    ExternalEpisode(
                        id = "$id/${episode.id}",
                        number = index + episodes.size + 1,
                        title = episode.title
                            ?: episode.titleRomanji
                            ?: episode.titleJapanese,
                        airDate = parseApiDate(episode.aired)
                    )
                }
            )
        } while (hasNextPage)

        return episodes
    }

    /**
     * Performs a rate-limited GET and returns the body.
     *
     * A `429` answer waits for the `Retry-After` delay and retries once.
     *
     * @param path endpoint path appended to [BASE_URL].
     * @param params query parameters of the request.
     * @return the response body as text.
     * @see [rateLimiter]
     */
    private suspend fun get(path: String, params: Map<String, String> = emptyMap()): String {
        rateLimiter.throttle()
        val response = httpClient.get("$BASE_URL$path") {
            params.forEach { (name, value) -> parameter(name, value) }
        }

        if (response.status == HttpStatusCode.TooManyRequests) {
            val retryAfter = response.headers["Retry-After"]?.toLongOrNull()
            val waitSeconds = retryAfter?.coerceAtLeast(1L) ?: DEFAULT_RETRY_SECONDS
            logger.warning("MyAnimeList rate limited, retrying in ${waitSeconds}s")
            delay(waitSeconds.seconds)
            return get(path)
        }

        require(response.status == HttpStatusCode.OK) {
            "MyAnimeList request failed for $path (${response.status.value})"
        }
        return response.bodyAsText()
    }

    /**
     * Maps a media entry to the normalized external representation.
     *
     * Every known title is collected into a set, the first air date is reduced to its year,
     * and the declared external links are turned into cross-platform identifiers.
     *
     * @return the [ExternalAnime] built from this entry.
     * @see [ExternalAnime]
     */
    internal fun MyAnimeListMedia.toExternalAnime(): ExternalAnime {
        val titles = titles.map(MyAnimeListTitle::title).toSet()
        val year = aired?.from?.take(4)?.toIntOrNull()

        return ExternalAnime(
            platform = MetadataPlatform.MYANIMELIST,
            id = id.toString(),
            titles = titles,
            description = synopsis,
            episodeCount = episodes,
            year = year,
            episodes = emptyList(),
            externalIds = externalLinks.mapNotNull { link ->
                val url = link.url ?: return@mapNotNull null
                when {
                    "anilist" in link.name.lowercase() || "anilist" in url ->
                        ANILIST_ID_REGEX.find(url)?.groupValues?.get(1)
                            ?.let { MetadataPlatform.ANILIST to it }
                    "anidb" in link.name.lowercase() || "anidb" in url ->
                        ANIDB_ID_REGEX.find(url)?.groupValues?.get(1)
                            ?.let { MetadataPlatform.ANIDB to it }
                    else -> null
                }
            }.toMap()
        )
    }

    /**
     * Parses the loose date formats of the API: a bare year, a year-month pair or a full
     * timestamp all reduce to the first air date.
     *
     * @param raw raw date string as returned by the API.
     * @return the parsed date, or `null` when it cannot be understood.
     */
    private fun parseApiDate(raw: String?): LocalDate? {
        if (raw.isNullOrBlank()) return null
        return runCatching {
            when (raw.length) {
                4 -> LocalDate.of(raw.toInt(), 1, 1)
                7 -> LocalDate.of(raw.take(4).toInt(), raw.substring(5).toInt(), 1)
                10 -> LocalDate.parse(raw)
                else -> ZonedDateTime.parse(raw).toLocalDate()
            }
        }.getOrNull()
    }

    companion object {
        private const val BASE_URL = "https://api.tenrai.org"
        private const val RATE_LIMIT_PER_MINUTE = 60
        private const val DEFAULT_RETRY_SECONDS = 60L
        private const val SEARCH_LIMIT = 10
        private val CACHE_TTL: Duration = 7.days

        private val ANILIST_ID_REGEX = Regex("""anilist\.co/anime/(\d+)""")
        private val ANIDB_ID_REGEX = Regex("""anidb\.net(?:/anime/|.*[?&]a?id=)(\d+)""")
    }
}
