package fr.shikkanime.utils

import com.google.common.cache.CacheBuilder
import com.google.common.cache.CacheLoader
import com.google.common.cache.LoadingCache
import java.time.Duration

class MapCache<K : Any, V>(
    private var duration: Duration? = null,
    private val classes: List<Class<*>> = listOf(),
    private val defaultKeys: List<K> = listOf(),
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
                return block(key)!!
            }
        })
    }

    private fun loadDefaultKeys() {
        defaultKeys.forEach { this[it] }
    }

    fun containsKey(key: K) = cache.getIfPresent(key) != null

    operator fun get(key: K): V? {
        return try {
            cache.getUnchecked(key)
        } catch (_: Exception) {
            null
        }
    }

    fun setIfNotExists(key: K, value: V & Any) {
        if (!containsKey(key))
            cache.put(key, value)
    }

    @Synchronized
    fun invalidate() {
        cache.invalidateAll()
        loadDefaultKeys()
    }

    companion object {
        private val globalCaches: MutableList<MapCache<*, *>> = mutableListOf()

        fun loadAll() {
            globalCaches.forEach { it.loadDefaultKeys() }
        }

        @Synchronized
        fun invalidate(vararg classes: Class<*>) {
            globalCaches.filter { it.classes.any { clazz -> classes.contains(clazz) } }
                .forEach { it.invalidate() }
        }

        // For test only
        @Synchronized
        fun invalidateAll() {
            globalCaches.forEach { it.invalidate() }
        }
    }
}