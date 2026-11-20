package fr.shikkanime.jobs.metadata

import fr.shikkanime.core.LoggerFactory
import fr.shikkanime.jobs.metadata.AniListClient.Companion.RATE_LIMIT_PER_MINUTE
import fr.shikkanime.ktor.createHttpClient
import fr.shikkanime.models.MetadataPlatform
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.delay
import org.koin.core.annotation.Single
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.seconds

/**
 * Client of the AniList GraphQL API.
 *
 * Every call goes through the shared [MetadataApiCache] and the [RateLimiter]: the platform
 * answers 429 bursts with temporary bans, so a request is only ever sent once per cache window
 * and at most [RATE_LIMIT_PER_MINUTE] times per minute.
 *
 * AniList exposes no per-episode entries: episodes are only reachable through the episode
 * count of the media.
 */
@Single
class AniListClient(
    private val cache: MetadataApiCache,
    private val rateLimiter: RateLimiter = RateLimiter(RATE_LIMIT_PER_MINUTE)
) {
    private val logger = LoggerFactory.getLogger()
    private val httpClient = createHttpClient()

    /**
     * Searches anime entries by title.
     *
     * @param query free-text search, matched by AniList against every known title.
     * @return the matching entries, best match first.
     */
    suspend fun searchAnime(query: String): List<ExternalAnime> {
        val body = cache.getOrPut(
            cache.key(MetadataPlatform.ANILIST, "search", query),
            CACHE_TTL
        ) {
            post(
                AniListRequest(
                    SEARCH_QUERY,
                    AniListVariables(search = query, page = 1, perPage = PER_PAGE)
                )
            )
        }
        return cache.decode<AniListPageResponse>(body)
            .data?.page?.media.orEmpty()
            .map { it.toExternalAnime() }
    }

    /**
     * Loads anime entries by their AniList identifiers.
     *
     * @param ids AniList media identifiers.
     * @return the matching entries.
     */
    suspend fun getAnimeByIds(ids: Collection<Int>): List<ExternalAnime> {
        if (ids.isEmpty()) return emptyList()
        val body = cache.getOrPut(
            cache.key(MetadataPlatform.ANILIST, "media", ids.sorted().joinToString(",")),
            CACHE_TTL
        ) {
            post(
                AniListRequest(
                    MEDIA_BY_IDS_QUERY,
                    AniListVariables(idIn = ids.toList(), page = 1, perPage = ids.size)
                )
            )
        }
        return cache.decode<AniListPageResponse>(body)
            .data?.page?.media.orEmpty()
            .map { it.toExternalAnime() }
    }

    /**
     * Performs a rate-limited GraphQL POST and returns the body.
     *
     * A `429` answer waits for the `Retry-After` or `X-RateLimit-Reset` delay and retries
     * once.
     *
     * @param request GraphQL request to send.
     * @return the response body as text.
     * @see [rateLimiter]
     */
    private suspend fun post(request: AniListRequest): String {
        rateLimiter.throttle()
        val response = httpClient.post(ANILIST_ENDPOINT) {
            contentType(ContentType.Application.Json)
            setBody(request)
        }

        if (response.status == HttpStatusCode.TooManyRequests) {
            val resetAt = response.headers["X-RateLimit-Reset"]?.toLongOrNull()
            val retryAfter = response.headers["Retry-After"]?.toLongOrNull()
            val waitSeconds = (resetAt?.minus(System.currentTimeMillis() / 1000))
                ?.coerceAtLeast(1L)
                ?: retryAfter?.coerceAtLeast(1L)
                ?: DEFAULT_RETRY_SECONDS
            logger.warning("AniList rate limited, retrying in ${waitSeconds}s")
            delay(waitSeconds.seconds)
            return post(request)
        }

        require(response.status == HttpStatusCode.OK) {
            "AniList request failed (${response.status.value})"
        }
        return response.bodyAsText()
    }

    /**
     * Maps a media entry to the normalized external representation.
     *
     * Every known title is collected into a set, the HTML description is stripped, and the
     * relations of anime type become related identifiers. AniList exposes no per-episode
     * entries, so the episode list stays empty.
     *
     * @return the [ExternalAnime] built from this entry.
     * @see [ExternalAnime]
     */
    internal fun AniListMedia.toExternalAnime(): ExternalAnime {
        val titles = buildSet {
            title?.romaji?.let(::add)
            title?.english?.let(::add)
            title?.native?.let(::add)
            synonyms?.let(::addAll)
        }

        return ExternalAnime(
            platform = MetadataPlatform.ANILIST,
            id = id.toString(),
            titles = titles,
            description = description?.let(::stripHtml),
            episodeCount = episodes,
            year = startDate?.year,
            episodes = emptyList(),
            externalIds = externalIds(),
            relatedIds = relations?.edges.orEmpty()
                .filter { it.node?.type == "ANIME" }
                .mapNotNull { it.node?.id?.toString() }
                .toSet()
        )
    }

    /**
     * Extracts the cross-platform identifiers declared by this media entry.
     *
     * `idMal` is the canonical MyAnimeList identifier; the external links are scanned for
     * MyAnimeList and AniDB URLs as a fallback.
     *
     * @return the external identifiers by platform, empty when none is declared.
     * @see [MetadataPlatform]
     */
    internal fun AniListMedia.externalIds(): Map<MetadataPlatform, String> {
        val ids = linkedMapOf<MetadataPlatform, String>()
        // idMal is the canonical MAL identifier of the entry
        idMal?.let { ids[MetadataPlatform.MYANIMELIST] = it.toString() }
        externalLinks.orEmpty().forEach { link ->
            val site = link.site?.lowercase() ?: return@forEach
            val url = link.url ?: return@forEach
            when {
                "myanimelist" in site -> MAL_ID_REGEX.find(url)?.groupValues?.get(1)
                    ?.let { ids.putIfAbsent(MetadataPlatform.MYANIMELIST, it) }
                "anidb" in site -> ANIDB_ID_REGEX.find(url)?.groupValues?.get(1)
                    ?.let { ids[MetadataPlatform.ANIDB] = it }
            }
        }
        return ids
    }

    companion object {
        private const val ANILIST_ENDPOINT = "https://graphql.anilist.co"
        private const val RATE_LIMIT_PER_MINUTE = 30
        private const val PER_PAGE = 10
        private const val DEFAULT_RETRY_SECONDS = 60L
        private val CACHE_TTL: Duration = 7.days

        private val HTML_TAG_REGEX = Regex("<[^>]+>")
        private val HTML_SPACE_REGEX = Regex("\\s+")
        private val MAL_ID_REGEX = Regex("""myanimelist\.net/anime/(\d+)""")
        private val ANIDB_ID_REGEX = Regex("""anidb\.net(?:/anime/|.*[?&]a?id=)(\d+)""")

        private val MEDIA_FIELDS = """
            id idMal format title { romaji english native } synonyms description episodes
            duration startDate { year month day } externalLinks { site url }
            relations { edges { relationType node { id type format title { romaji english native } } } }
        """.trimIndent().replace(Regex("\\s+"), " ")

        private val SEARCH_QUERY = $$"""
            query ($search: String, $page: Int, $perPage: Int) {
              Page(page: $page, perPage: $perPage) {
                media(search: $search, type: ANIME, sort: SEARCH_MATCH,
                      status_in: [NOT_YET_RELEASED, RELEASING, FINISHED],
                      format_in: [TV, TV_SHORT, MOVIE, SPECIAL, OVA, ONA]) { $$MEDIA_FIELDS }
              }
            }
        """.trimIndent().replace(Regex("\\s+"), " ")

        private val MEDIA_BY_IDS_QUERY = $$"""
            query ($idIn: [Int], $page: Int, $perPage: Int) {
              Page(page: $page, perPage: $perPage) {
                media(id_in: $idIn, type: ANIME,
                      status_in: [NOT_YET_RELEASED, RELEASING, FINISHED],
                      format_in: [TV, TV_SHORT, MOVIE, SPECIAL, OVA, ONA]) { $$MEDIA_FIELDS }
              }
            }
        """.trimIndent().replace(Regex("\\s+"), " ")
    }

    /**
     * Strips the HTML markup of a description.
     *
     * Line-break tags become spaces, every other tag is dropped, the common entities are
     * decoded and the runs of whitespace are collapsed.
     *
     * @receiver HTML description as returned by the API.
     * @return the plain text of the description.
     */
    private fun stripHtml(html: String): String =
        html.replace(Regex("<br\\s*/?>", RegexOption.IGNORE_CASE), " ")
            .replace(HTML_TAG_REGEX, "")
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&#039;", "'")
            .replace(HTML_SPACE_REGEX, " ")
            .trim()
}
