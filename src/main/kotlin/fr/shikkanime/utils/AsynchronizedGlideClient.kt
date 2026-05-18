package fr.shikkanime.utils

import fr.shikkanime.utils.system.CircuitBreaker
import glide.api.GlideClient
import glide.api.models.commands.scan.ScanOptions
import glide.api.models.configuration.GlideClientConfiguration
import glide.api.models.configuration.NodeAddress
import io.ktor.util.collections.*
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap

object AsynchronizedGlideClient {
    private val logger = LoggerFactory.getLogger(AsynchronizedGlideClient::class.java)
    private val circuitBreaker = CircuitBreaker("Valkey", 3, Duration.ofMinutes(1))
    private val inMemoryCache = ConcurrentHashMap<String, String>()
    private val keysDeletePool = ConcurrentSet<String>()
    private val glideClient = runCatching {
        GlideClient.createClient(
            GlideClientConfiguration.builder()
                .address(NodeAddress.builder().host(Constant.valkeyHost).port(Constant.valkeyPort).build())
                .requestTimeout(5000)
                .build()
        ).get()
    }

    init {
        if (!isAvailable())
            logger.warning("Can not access to Valkey, using in-memory cache instead of persistent cache: ${glideClient.exceptionOrNull()?.message}")
    }

    fun isAvailable() = circuitBreaker.isAvailable() && glideClient.isSuccess

    operator fun get(key: String): String? {
        return circuitBreaker.execute(
            action = {
                val value = glideClient.getOrThrow().get(key).get()?.takeIfNotBlank()
                // If valkey is back, check if we have a value in memory to update it
                if (value != null && inMemoryCache.containsKey(key)) {
                    set(key, value)
                    inMemoryCache.remove(key)
                }
                value
            },
            fallback = { inMemoryCache[key] }
        )
    }

    operator fun set(key: String, value: String) {
        circuitBreaker.execute(
            action = { glideClient.getOrThrow().set(key, value).get() },
            fallback = { inMemoryCache[key] = value }
        )
    }

    fun delInPool(keys: Collection<String>) {
        circuitBreaker.execute(
            action = { keysDeletePool.addAll(keys) },
            fallback = { keys.forEach { inMemoryCache.remove(it) } }
        )
    }

    fun del(keys: Array<String>) {
        circuitBreaker.execute(
            action = { glideClient.getOrThrow().del(keys).get() },
            fallback = { keys.forEach { inMemoryCache.remove(it) } }
        )
    }

    fun del() = synchronized(this) {
        val copy = keysDeletePool.toList()
        if (copy.isEmpty()) return@synchronized

        circuitBreaker.execute(
            action = {
                copy.chunked(1000).forEach { chunk -> glideClient.getOrThrow().del(chunk.toTypedArray()).get() }
                keysDeletePool.removeAll(copy.toSet())
            },
            fallback = {
                copy.forEach { inMemoryCache.remove(it) }
                keysDeletePool.removeAll(copy.toSet())
            }
        )
    }

    fun searchAll(pattern: String, count: Long = 5000): List<String> {
        return circuitBreaker.execute(
            action = {
                val options = ScanOptions.builder().matchPattern(pattern).count(count).build()

                val allKeys = mutableListOf<String>()
                var cursor = "0"

                do {
                    val result = glideClient.getOrThrow().scan(cursor, options).get()
                    val arrays = result.filterIsInstance<Array<*>>()
                    val keys = arrays.flatMap { it.filterIsInstance<String>() }
                    allKeys += keys

                    val newCursor = result.filterIsInstance<String>().firstOrNull()
                    cursor = newCursor ?: "0"
                } while (cursor != "0")

                allKeys
            },
            fallback = {
                inMemoryCache.keys().toList().filter { it.matches(Regex(pattern.replace("*", ".*"))) }
            }
        )
    }
}