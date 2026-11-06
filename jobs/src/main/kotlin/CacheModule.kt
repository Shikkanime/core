package fr.shikkanime.jobs

import fr.shikkanime.cache.Cache
import fr.shikkanime.cache.ValkeyWrapperImpl
import fr.shikkanime.core.LoggerFactory
import kotlinx.coroutines.runBlocking
import org.koin.core.annotation.Module
import org.koin.core.annotation.Single

private val logger = LoggerFactory.getLogger("CacheModule")

/**
 * Provides the framework cache backed by the Valkey instance named by `VALKEY_HOST` and
 * `VALKEY_PORT`.
 *
 * The job only invalidates buckets here: it writes the catalog, so its cache handles only the
 * version counters the API instances read. When Valkey is unreachable the job keeps writing and
 * uses a no-op client: the API entries then expire with their TTL instead of failing the harvest.
 *
 * @see [Cache]
 */
@Suppress("unused") // Bound by Koin and started from the application graph
@Module
internal class CacheModule {
    /**
     * Connects the cache facade to Valkey, falling back to a no-op client when it is unreachable.
     *
     * @return the cache holding the version counters of the catalog buckets.
     * @see [ValkeyWrapperImpl.connect]
     */
    @Single
    fun cache(): Cache =
        runBlocking {
            val valkeyWrapper = try {
                ValkeyWrapperImpl.connect(
                    host = System.getenv("VALKEY_HOST")?.takeIf(String::isNotBlank) ?: "localhost",
                    port = System.getenv("VALKEY_PORT")?.toIntOrNull() ?: 6379
                )
            } catch (exception: Exception) {
                logger.warning(
                    "Valkey is unreachable, the catalog cache will not be invalidated: ${exception.message}"
                )
                NoopValkeyWrapper
            }

            Cache(valkeyWrapper = valkeyWrapper)
        }
}
