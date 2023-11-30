package fr.shikkanime.utils

import com.google.common.cache.CacheBuilder
import com.google.common.cache.CacheLoader
import com.google.common.cache.LoadingCache
import java.time.Duration

class MapCache<K : Any, V>(
    duration: Duration? = null,
    private val classes: List<Class<*>> = listOf(),
    private val block: (K) -> V,
) {
    private val cache: LoadingCache<K, V>

    init {
        val builder = CacheBuilder.newBuilder()

        if (duration != null) {
            builder.expireAfterWrite(duration)
        }

        cache = builder
            .build(object : CacheLoader<K, V>() {
                override fun load(key: K): V {
                    return block(key)
                }
            })

        caches.add(this)
    }

    operator fun get(key: K): V {
        return cache.getUnchecked(key)
    }

    companion object {
        private val caches: MutableList<MapCache<*, *>> = mutableListOf()

        fun invalidate(vararg classes: Class<*>) {
            classes.forEach { clazz ->
                caches.filter { it.classes.contains(clazz) }.forEach { it.cache.invalidateAll() }
            }
        }
    }
}