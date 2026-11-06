package fr.shikkanime.jobs

import fr.shikkanime.cache.Cache
import fr.shikkanime.cache.ValkeyWrapperImpl
import kotlinx.coroutines.runBlocking
import org.koin.core.annotation.Module
import org.koin.core.annotation.Single

/**
 * Provides the framework cache backed by the Valkey instance named by `VALKEY_HOST` and
 * `VALKEY_PORT`.
 *
 * The job only invalidates buckets here: it writes the catalog, so its cache handles only the
 * version counters the API instances read.
 *
 * @see [Cache]
 */
@Suppress("unused") // Bound by Koin and started from the application graph
@Module
internal class CacheModule {
    /**
     * Connects the cache facade to Valkey.
     *
     * @return the cache holding the version counters of the catalog buckets.
     * @see [ValkeyWrapperImpl.connect]
     */
    @Single
    fun cache(): Cache =
        runBlocking {
            Cache(
                valkeyWrapper = ValkeyWrapperImpl.connect(
                    host = System.getenv("VALKEY_HOST")?.takeIf(String::isNotBlank) ?: "localhost",
                    port = System.getenv("VALKEY_PORT")?.toIntOrNull() ?: 6379
                )
            )
        }
}
