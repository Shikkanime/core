package fr.shikkanime.utils

import com.google.common.cache.CacheBuilder
import com.google.common.cache.CacheLoader
import com.google.common.cache.LoadingCache
import java.time.Duration

class MapCache<K : Any, V>(
    private var duration: Duration? = null,
    private val classes: List<Class<*>> = listOf(),
    private val log: Boolean = true,
    private val block: (K) -> V,
) {
    private lateinit var cache: LoadingCache<K, V>

    init {
        setCache()
        globalCaches.add(this)
    }

    private fun MapCache<K, V>.setCache() {
        val builder = CacheBuilder.newBuilder().apply {
            duration?.let { expireAfterWrite(it) }
        }

        cache = builder.build(object : CacheLoader<K, V & Any>() {
            override fun load(key: K): V & Any {
                if (log) logger.info("Loading $key")
                return block(key)!!
            }
        })
    }

    operator fun get(key: K): V? {
        return try {
            cache.getUnchecked(key)
        } catch (_: Exception) {
            null
        }
    }

    operator fun set(key: K, value: V & Any) {
        cache.put(key, value)
    }

    fun invalidate() {
        cache.invalidateAll()
    }

    companion object {
        private val logger = LoggerFactory.getLogger(MapCache::class.java)
        private val globalCaches: MutableList<MapCache<*, *>> = mutableListOf()

        fun invalidate(vararg classes: Class<*>) {
            globalCaches.filter { it.classes.any { clazz -> classes.contains(clazz) } }
                .forEach { it.invalidate() }
        }

        // For test only
        fun invalidateAll() {
            globalCaches.forEach { it.invalidate() }
        }
    }
}