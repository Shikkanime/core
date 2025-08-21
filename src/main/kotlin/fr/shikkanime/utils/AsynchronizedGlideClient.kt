package fr.shikkanime.utils

import glide.api.GlideClient
import glide.api.models.commands.scan.ScanOptions
import glide.api.models.configuration.GlideClientConfiguration
import glide.api.models.configuration.NodeAddress

object AsynchronizedGlideClient {
    private val logger = LoggerFactory.getLogger(AsynchronizedGlideClient::class.java)
    private val glideClient = runCatching {
        GlideClient.createClient(
            GlideClientConfiguration.builder()
                .address(NodeAddress.builder().host(Constant.valkeyHost).port(Constant.valkeyPort).build())
                .build()
        ).get()
    }

    init {
        if (!isAvailable())
            logger.warning("Can not access to Valkey, using in-memory cache instead of persistent cache: ${glideClient.exceptionOrNull()?.message}")
    }

    fun isAvailable() = glideClient.isSuccess

    operator fun get(key: String): String? = glideClient.getOrNull()?.get(key)?.get()?.takeIf { it.isNotBlank() }

    operator fun set(key: String, value: String) = glideClient.getOrNull()?.set(key, value)?.get()

    fun del(keys: Array<String>) = glideClient.getOrNull()?.del(keys)?.get()

    fun search(pattern: String, count: Long = 1000): List<String> = glideClient.getOrNull()?.scan("0", ScanOptions.builder().matchPattern(pattern).count(count).build())?.get()
        ?.filterIsInstance<Array<*>>()
        ?.flatMap { it.mapNotNull { key -> key as? String } }
        ?: emptyList()
}