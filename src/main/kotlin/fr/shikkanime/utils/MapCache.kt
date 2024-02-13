package fr.shikkanime.utils

import com.google.common.cache.CacheBuilder
import com.google.common.cache.CacheLoader
import com.google.common.cache.LoadingCache
import java.time.Duration

class MapCache<K : Any, V>(
    private var duration: Duration? = null,
    private val classes: List<Class<*>> = listOf(),
    private val block: (K) -> V,
) {
    private lateinit var cache: LoadingCache<K, V>

    init {
        setCache()
        globalCaches.add(this)
    }

    private fun MapCache<K, V>.setCache() {
        val builder = CacheBuilder.newBuilder()

        if (duration != null) {
            builder.expireAfterWrite(duration!!)
        }

        cache = builder
            .build(object : CacheLoader<K, V & Any>() {
                override fun load(key: K): V & Any {
                    logger.info("Loading $key")
                    return block(key)!!
                }
            })
    }

    operator fun get(key: K): V? {
        return try {
            cache.getUnchecked(key)
        } catch (e: Exception) {
            null
        }
    }

    operator fun set(key: K, value: V & Any) {
        cache.put(key, value)
    }

    fun resetWithNewDuration(duration: Duration) {
        this.duration = duration
        setCache()
    }

    fun invalidate() {
        cache.invalidateAll()
    }

    companion object {
        private val logger = LoggerFactory.getLogger(MapCache::class.java)
        private val globalCaches: MutableList<MapCache<*, *>> = mutableListOf()

        fun invalidate(vararg classes: Class<*>) {
            classes.forEach { clazz ->
                globalCaches.filter { it.classes.contains(clazz) }.forEach { it.invalidate() }
            }
        }
    }
}