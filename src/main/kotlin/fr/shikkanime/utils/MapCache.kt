package fr.shikkanime.utils

import io.ktor.util.collections.*
import java.time.Duration

private val defaultCacheDuration: Duration = Duration.ofDays(1)

class MapCache<K : Any, V>(
    name: String,
    private var duration: Duration = defaultCacheDuration,
    private val classes: List<Class<*>> = emptyList(),
    private val block: (K) -> V,
) {
    private val cache: ConcurrentMap<K, Pair<Long, V>> = ConcurrentMap()

    init {
        globalCaches[name] = this
    }

    fun containsKey(key: K) = cache.containsKey(key)

    fun containsKeyAndValid(key: K) = cache.containsKey(key) && !needInvalidation(key)

    private fun needInvalidation(key: K): Boolean {
        val (timestamp, _) = cache[key] ?: return false
        return System.currentTimeMillis() - timestamp > duration.toMillis()
    }

    operator fun get(key: K): V? {
        val cachedValue = cache[key]

        if (cachedValue == null || needInvalidation(key)) {
            val newValue = block(key)
            cache[key] = System.currentTimeMillis() to newValue
            return newValue
        }

        return cachedValue.second
    }

    fun setIfNotExists(key: K, value: V & Any) {
        if (!containsKey(key))
            cache.put(key, System.currentTimeMillis() to value)
    }

    fun invalidate() {
        cache.clear()
    }

    fun removeInvalidated() {
        val now = System.currentTimeMillis()

        cache.filter { (_, value) -> now - value.first > duration.toMillis() }
            .toMap()
            .forEach { (key, _) -> cache.remove(key) }
    }

    companion object {
        private val globalCaches: ConcurrentMap<String, MapCache<*, *>> = ConcurrentMap()

        fun invalidate(vararg classes: Class<*>) {
            globalCaches.forEach { (_, cache) ->
                if (cache.classes.any { clazz -> clazz in classes })
                    cache.invalidate()
                cache.removeInvalidated()
            }
        }

        // For test only
        fun invalidateAll() {
            globalCaches.forEach { (_, cache) ->
                cache.invalidate()
            }
        }

        fun <K : Any, V : Any?> getOrComputeNullable(
            name: String,
            duration: Duration = defaultCacheDuration,
            classes: List<Class<*>> = emptyList(),
            key: K,
            block: (K) -> V
        ): V? {
            @Suppress("UNCHECKED_CAST")
            val mapCache = globalCaches[name] as? MapCache<K, V> ?: MapCache(name, duration, classes, block)
            return mapCache[key]
        }

        fun <K : Any, V> getOrCompute(
            name: String,
            duration: Duration = defaultCacheDuration,
            classes: List<Class<*>> = emptyList(),
            key: K,
            block: (K) -> V
        ) = getOrComputeNullable(name, duration, classes, key, block)!!
    }
}