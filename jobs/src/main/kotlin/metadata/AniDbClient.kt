package fr.shikkanime.jobs.metadata

import fr.shikkanime.core.LoggerFactory
import fr.shikkanime.jobs.metadata.AniDbClient.Companion.RATE_LIMIT_PER_MINUTE
import fr.shikkanime.jobs.metadata.AniDbClient.Companion.TITLE_INDEX_TTL
import fr.shikkanime.ktor.createHttpClient
import fr.shikkanime.models.MetadataPlatform
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import org.jsoup.parser.Parser
import org.koin.core.annotation.Single
import java.io.ByteArrayInputStream
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.zip.GZIPInputStream
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.seconds

/**
 * Client of the AniDB HTTP API.
 *
 * Every call goes through the shared [MetadataApiCache] and the [RateLimiter]: AniDB bans
 * aggressive clients hard, so a request is only ever sent once per cache window and at most
 * [RATE_LIMIT_PER_MINUTE] times per minute. The title index is downloaded once and cached for
 * [TITLE_INDEX_TTL], turning the per-anime search into a local scan.
 */
@Single
class AniDbClient(
    private val cache: MetadataApiCache,
    private val rateLimiter: RateLimiter = RateLimiter(RATE_LIMIT_PER_MINUTE)
) {
    private val logger = LoggerFactory.getLogger()
    private val httpClient = createHttpClient()

    /**
     * Searches the AniDB title index for entries matching [query].
     *
     * The index lists every known title of every entry; the best title matches are returned as
     * stub entries the caller can resolve with [getAnimeByIds].
     *
     * @param query free-text title to look up.
     * @return the matching entries, best match first.
     */
    suspend fun searchAnime(query: String): List<ExternalAnime> {
        val xml = cache.getOrPut(
            cache.key(MetadataPlatform.ANIDB, "titles"),
            TITLE_INDEX_TTL
        ) {
            fetch(TITLES_URL)
        }
        return searchTitleIndex(xml, query)
    }

    /**
     * Scores every entry of the title index against [query].
     *
     * @param xml raw `anime-titles.xml` payload.
     * @param query title to look up.
     * @return the matching entries, best match first.
     */
    internal fun searchTitleIndex(xml: String, query: String): List<ExternalAnime> {
        val document = Jsoup.parse(xml, "", Parser.xmlParser())

        // The index holds hundreds of thousands of entries: a cheap lexical pass prefilters
        // them, then only the survivors pay for the edit-distance refinement.
        return document.select("animetitles > anime").asSequence().mapNotNull { element ->
            val aid = element.attr("aid").toIntOrNull() ?: return@mapNotNull null
            val titles = element.select("title").map { it.text().trim() }.toSet()
            val fastScore = TitleMatcher.bestScoreFast(titles, query)

            // Prefilter on the cheap score: spelling variants only emerge from the
            // edit-distance refinement of [TitleMatcher.score], applied to the survivors.
            if (fastScore < MIN_SEARCH_PREFILTER) return@mapNotNull null
            ExternalAnime(
                platform = MetadataPlatform.ANIDB,
                id = aid.toString(),
                titles = titles,
                description = null,
                episodeCount = null,
                year = null,
                episodes = emptyList(),
                relatedIds = emptySet()
            )
        }
            .map { it to TitleMatcher.bestScore(it.titles, query) }
            .filter { it.second >= MIN_SEARCH_SCORE }
            .sortedByDescending { it.second }
            .take(MAX_SEARCH_RESULTS)
            .map { it.first }
            .toList()
    }

    /**
     * Loads full anime entries by their AniDB identifiers.
     *
     * @param ids AniDB anime identifiers (`aid`).
     * @return the matching entries, unresolvable ones excluded.
     */
    suspend fun getAnimeByIds(ids: Collection<Int>): List<ExternalAnime> =
        ids.mapNotNull { id ->
            val body = cache.getOrPut(
                cache.key(MetadataPlatform.ANIDB, "anime", id.toString()),
                CACHE_TTL
            ) {
                fetch("$API_URL?request=anime&client=shikkanime&clientver=1&protover=1&aid=$id")
            }
            runCatching { parseAnimeDocument(body, id) }
                .onFailure { logger.warning("AniDB entry $id could not be parsed: ${it.message}") }
                .getOrNull()
        }

    /**
     * Parses one `anime` document of the AniDB API.
     *
     * Titles, plot, episode entries, cross-platform identifiers and related entries are read
     * from the XML payload; an `error` element or a missing title list means the entry is
     * unusable and yields `null`.
     *
     * @param xml raw `anime` payload of the API.
     * @param aid AniDB identifier the payload was fetched for.
     * @return the parsed entry, or `null` when the payload carries no usable anime.
     * @see [ExternalAnime]
     */
    internal fun parseAnimeDocument(xml: String, aid: Int): ExternalAnime? {
        val document = Jsoup.parse(xml, "", Parser.xmlParser())
        if (document.select("error").isNotEmpty())
            return null

        val anime = document.selectFirst("anime") ?: return null
        val titles = anime.select("titles > title").map { it.text().trim() }.toSet()
        if (titles.isEmpty()) return null

        val episodes = anime.select("episodes > episode").mapNotNull { element ->
            val episodeId = element.attr("id")
            if (episodeId.isBlank()) return@mapNotNull null
            // epno type 1 is the episode number; other types carry cour or part markers
            val epno = element.select("epno[type=1]").firstOrNull()?.text()?.trim()
                ?: element.select("epno").firstOrNull()?.text()?.trim()
            ExternalEpisode(
                id = episodeId,
                number = epno?.toIntOrNull(),
                title = element.select("title").first()?.text()?.trim()?.ifBlank { null },
                airDate = element.select("airdate").first()?.text()?.trim()
                    ?.takeIf { it.isNotBlank() }
                    ?.let { runCatching { LocalDate.parse(it, DATE_FORMATTER) }.getOrNull() }
            )
        }

        val externalIds = anime.select("resources > resource").mapNotNull { resource ->
            val identifier = resource.selectFirst("externalentity > identifier")?.text()?.trim()
                ?.toIntOrNull() ?: return@mapNotNull null
            when (resource.attr("type")) {
                RESOURCE_TYPE_MAL -> MetadataPlatform.MYANIMELIST to identifier.toString()
                else -> null
            }
        }.toMap()

        return ExternalAnime(
            platform = MetadataPlatform.ANIDB,
            id = aid.toString(),
            titles = titles,
            description = anime.selectFirst("plot")?.text()?.trim()?.ifBlank { null },
            episodeCount = episodes.size.takeIf { it > 0 },
            year = anime.selectFirst("startdate")?.text()?.trim()
                ?.let { runCatching { LocalDate.parse(it, DATE_FORMATTER).year }.getOrNull() },
            episodes = episodes,
            externalIds = externalIds,
            relatedIds = anime.select("relatedanime > anime").mapNotNull { it.attr("id") }
                .filter { it.isNotBlank() }
                .toSet()
        )
    }

    /**
     * Performs a rate-limited GET and returns the decoded body.
     *
     * AniDB serves several payloads as gzip files rather than gzip content-encoding, so the
     * magic bytes decide whether the body needs decompressing.
     *
     * @param url absolute URL to GET.
     * @return the decoded body as text.
     * @see [rateLimiter]
     */
    private suspend fun fetch(url: String): String {
        rateLimiter.throttle()
        val response = httpClient.get(url)

        if (response.status == HttpStatusCode.TooManyRequests) {
            val retryAfter = response.headers["Retry-After"]?.toLongOrNull()
            val waitSeconds = retryAfter?.coerceAtLeast(1L) ?: DEFAULT_RETRY_SECONDS
            logger.warning("AniDB rate limited, retrying in ${waitSeconds}s")
            delay(waitSeconds.seconds)
            return fetch(url)
        }

        require(response.status == HttpStatusCode.OK) {
            "AniDB request failed for $url (${response.status.value})"
        }

        val bytes = response.body<ByteArray>()
        val payload = if (bytes.size > 2 && bytes[0] == GZIP_MAGIC_FIRST && bytes[1] == GZIP_MAGIC_SECOND) {
            withContext(Dispatchers.IO) {
                GZIPInputStream(ByteArrayInputStream(bytes)).readBytes()
            }
        } else {
            bytes
        }
        return payload.decodeToString()
    }

    companion object {
        private const val API_URL = "http://api.anidb.net:9001/httpapi"
        private const val TITLES_URL = "https://anidb.net/api/anime-titles.xml.gz"
        private const val RATE_LIMIT_PER_MINUTE = 10
        private const val DEFAULT_RETRY_SECONDS = 60L
        private const val MAX_SEARCH_RESULTS = 8
        private const val MIN_SEARCH_SCORE = 0.5
        private const val MIN_SEARCH_PREFILTER = 0.3
        private const val RESOURCE_TYPE_MAL = "2"
        private const val GZIP_MAGIC_FIRST: Byte = 0x1f
        private const val GZIP_MAGIC_SECOND: Byte = 0x8b.toByte()
        private val CACHE_TTL: Duration = 7.days
        private val TITLE_INDEX_TTL: Duration = 7.days
        private val DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    }
}
