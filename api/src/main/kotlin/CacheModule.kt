package fr.shikkanime.api

import fr.shikkanime.cache.Cache
import fr.shikkanime.cache.ValkeyWrapperImpl
import kotlinx.coroutines.runBlocking
import org.koin.core.annotation.Module
import org.koin.core.annotation.Single

/**
 * Provides the framework cache backed by the Valkey instance named by `VALKEY_HOST` and
 * `VALKEY_PORT`.
 *
 * The defaults target a Valkey listening on `localhost:6379`; the Docker Compose stack sets
 * `VALKEY_HOST=valkey` and a deployment overrides both variables. The API fails fast when Valkey
 * is unreachable, because the cache is on its read path.
 *
 * @see [Cache]
 */
@Suppress("unused") // Bound by Koin and started from the application graph
@Module
internal class CacheModule {
    /**
     * Connects the two-level cache to Valkey.
     *
     * @return the cache shared by every API instance through the Valkey level.
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
