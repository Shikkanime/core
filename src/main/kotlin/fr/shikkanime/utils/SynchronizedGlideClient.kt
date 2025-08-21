package fr.shikkanime.utils

import glide.api.GlideClient
import glide.api.models.commands.scan.ScanOptions
import glide.api.models.configuration.GlideClientConfiguration
import glide.api.models.configuration.NodeAddress

object SynchronizedGlideClient {
    private val logger = LoggerFactory.getLogger(SynchronizedGlideClient::class.java)
    private val glideClient = runCatching {
        GlideClient.createClient(
            GlideClientConfiguration.builder()
                .address(NodeAddress.builder().host(Constant.valkeyHost).port(Constant.valkeyPort).build())
                .build()
        ).get()
    }.getOrNull()

    init {
        if (!isAvailable())
            logger.warning("Can not access to Valkey, using in-memory cache instead of persistent cache")
    }

    fun isAvailable() = glideClient != null

    operator fun get(key: String): String? = synchronized(this) {
        glideClient?.get(key)?.get()?.takeIf { it.isNotBlank() }
    }

    operator fun set(key: String, value: String) = synchronized(this) {
        glideClient?.set(key, value)?.get()
    }

    fun del(keys: Array<String>) = synchronized(this) {
        glideClient?.del(keys)?.get()
    }

    fun search(pattern: String, count: Long = 100): List<String> = synchronized(this) {
        glideClient?.scan("0", ScanOptions.builder().matchPattern(pattern).count(count).build())?.get()
            ?.filterIsInstance<Array<*>>()
            ?.flatMap { it.mapNotNull { key -> key as? String } }
            ?: emptyList()
    }
}