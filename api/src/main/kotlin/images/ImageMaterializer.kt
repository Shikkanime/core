package fr.shikkanime.api.images

import fr.shikkanime.database.services.ImagePayload
import fr.shikkanime.database.services.ImageService
import fr.shikkanime.database.services.ImageSource
import fr.shikkanime.models.Image
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.koin.core.annotation.Single
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days

private val REFRESH_TTL: Duration = 30.days

/**
 * Produces the artwork a request asks for, once, and revalidates it afterwards.
 *
 * The database stores the source URL of an artwork, never the picture: this class reads the
 * platform, hands the bytes to [ImageService] for resizing and encoding, and serializes the work per
 * artwork — a burst of concurrent requests must not fetch the same picture twenty times.
 *
 * An artwork older than [REFRESH_TTL] is revalidated after the response, with a conditional read: an
 * untouched platform costs a round trip, a changed one publishes a new version.
 *
 * @param imageService service storing and serving the artwork.
 * @param imageFetcher reader of the platform artwork.
 * @see [ImageService]
 */
@Single
internal class ImageMaterializer(
    private val imageService: ImageService,
    private val imageFetcher: ImageFetcher
) {
    private val locks = ConcurrentHashMap<String, Mutex>()
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    /**
     * Materializes an artwork from its platform when it was never asked for.
     *
     * @param registered image row to materialize.
     * @return the stored artwork with its payload, or `null` when the platform could not be read.
     * @see [ImagePayload]
     */
    suspend fun materialize(registered: Image): ImagePayload? =
        lock(registered).withLock {
            // Coalescing point: a request that waited for the lock must not fetch what the previous
            // one already stored
            val current = imageService.findByEntityIdAndType(
                requireNotNull(registered.entityId),
                registered.type
            ) ?: return@withLock null

            // The loser of the single flight serves what the winner just published; a version
            // without payload (database copied without it) falls through and is rebuilt
            if (current.version != null) {
                val payload = imageService.findPayload(current)
                if (payload != null)
                    return@withLock ImagePayload(current, payload)
            }

            when (val fetched = imageFetcher.fetch(current.url, null, null)) {
                is ImageFetch.Fetched -> {
                    // The store refuses the bytes when the harvest moved the artwork during the fetch
                    runCatching {
                        imageService.store(current, ImageSource(fetched.bytes, fetched.etag, fetched.lastModified))
                    }.getOrNull()
                }

                else -> {
                    imageService.recordFailure(current)
                    null
                }
            }
        }

    /**
     * Revalidates a served artwork once it is older than [REFRESH_TTL].
     *
     * The response is already on its way: the platform is read in the background, so a slow or
     * unreachable platform never slows a request down.
     *
     * @param image artwork currently served, with the validators of its last fetch.
     * @see [ImageService.store]
     */
    fun revalidateIfStale(image: Image) {
        val fetchedAt = image.fetchedAt ?: return
        if (Clock.System.now() - fetchedAt.toInstant(TimeZone.UTC) < REFRESH_TTL)
            return

        scope.launch {
            lock(image).withLock {
                // A burst on an expired artwork must not revalidate it once per request
                val current = imageService.findByEntityIdAndType(
                    requireNotNull(image.entityId),
                    image.type
                ) ?: return@withLock

                val refreshedAt = current.fetchedAt ?: return@withLock
                if (Clock.System.now() - refreshedAt.toInstant(TimeZone.UTC) < REFRESH_TTL)
                    return@withLock

                when (
                    val fetched = imageFetcher.fetch(
                        current.url,
                        current.sourceEtag,
                        current.sourceLastModified
                    )
                ) {
                    // The platform still serves the same bytes: only the freshness date moves
                    is ImageFetch.NotModified -> imageService.markFresh(current, fetched.etag, fetched.lastModified)

                    is ImageFetch.Fetched ->
                        runCatching {
                            imageService.store(current, ImageSource(fetched.bytes, fetched.etag, fetched.lastModified))
                        }

                    ImageFetch.Failed -> Unit
                }
            }
        }
    }

    private fun lock(image: Image): Mutex =
        locks.computeIfAbsent("${image.entityId}:${image.type.name}") { Mutex() }
}
