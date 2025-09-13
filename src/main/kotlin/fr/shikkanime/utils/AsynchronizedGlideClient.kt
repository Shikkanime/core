package fr.shikkanime.utils

import glide.api.GlideClient
import glide.api.models.commands.scan.ScanOptions
import glide.api.models.configuration.GlideClientConfiguration
import glide.api.models.configuration.NodeAddress
import io.ktor.util.collections.*

object AsynchronizedGlideClient {
    private val logger = LoggerFactory.getLogger(AsynchronizedGlideClient::class.java)
    private val glideClient = runCatching {
        GlideClient.createClient(
            GlideClientConfiguration.builder()
                .address(NodeAddress.builder().host(Constant.valkeyHost).port(Constant.valkeyPort).build())
                .requestTimeout(5000)
                .build()
        ).get()
    }
    private val keysDeletePool = ConcurrentSet<String>()

    init {
        if (!isAvailable())
            logger.warning("Can not access to Valkey, using in-memory cache instead of persistent cache: ${glideClient.exceptionOrNull()?.message}")
    }

    fun isAvailable() = glideClient.isSuccess

    operator fun get(key: String): String? = glideClient.getOrNull()?.get(key)?.get()?.takeIf { it.isNotBlank() }

    operator fun set(key: String, value: String) = glideClient.getOrNull()?.set(key, value)?.get()

    fun delInPool(keys: Collection<String>) = keysDeletePool.addAll(keys)

    fun del(keys: Array<String>) = glideClient.getOrNull()?.del(keys)?.get()

    fun del() = synchronized(this) {
        val copy = keysDeletePool.toList()
        if (copy.isEmpty()) return@synchronized

        val client = glideClient.getOrNull() ?: return@synchronized
        copy.chunked(1000).forEach { chunk -> client.del(chunk.toTypedArray()).get() }
        keysDeletePool.removeAll(copy.toSet())
    }

    fun searchAll(pattern: String, count: Long = 5000): List<String> {
        val client = glideClient.getOrNull() ?: return emptyList()
        val options = ScanOptions.builder().matchPattern(pattern).count(count).build()

        val allKeys = mutableListOf<String>()
        var cursor = "0"

        do {
            val result = client.scan(cursor, options).get() ?: break
            val arrays = result.filterIsInstance<Array<*>>()
            val keys = arrays.flatMap { it.mapNotNull { k -> k as? String } }
            allKeys += keys

            val newCursor = result.filterIsInstance<String>().firstOrNull()
            cursor = newCursor ?: "0"
        } while (cursor != "0")

        return allKeys
    }
}