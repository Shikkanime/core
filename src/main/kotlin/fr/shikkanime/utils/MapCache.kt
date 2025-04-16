package fr.shikkanime.utils

import io.ktor.util.collections.*
import java.time.Duration

class MapCache<K : Any, V>(
    name: String,
    private var duration: Duration? = null,
    private val classes: List<Class<*>> = emptyList(),
    private val block: (K) -> V,
) {
    private val cache: MutableMap<K, Pair<Long, V>> = mutableMapOf()

    init {
        globalCaches[name] = this
    }

    fun containsKey(key: K) = cache.containsKey(key)

    private fun needInvalidation(key: K): Boolean {
        if (duration != null) {
            val (timestamp, _) = cache[key] ?: return false
            return System.currentTimeMillis() - timestamp > duration!!.toMillis()
        }

        return false
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
        if (duration == null) return
        val now = System.currentTimeMillis()
        cache.entries.removeIf { (_, value) -> now - value.first > duration!!.toMillis() }
    }

    companion object {
        private val globalCaches: ConcurrentMap<String, MapCache<*, *>> = ConcurrentMap()

        fun invalidate(vararg classes: Class<*>) {
            globalCaches.forEach { (_, cache) ->
                if (cache.classes.any { clazz -> classes.contains(clazz) })
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
            duration: Duration? = null,
            classes: List<Class<*>> = emptyList(),
            key: K,
            block: (K) -> V
        ): V? {
            return globalCaches[name]?.let {
                @Suppress("UNCHECKED_CAST")
                (it as MapCache<K, V>)[key]
            } ?: MapCache(name, duration, classes, block)[key]
        }

        fun <K : Any, V> getOrCompute(
            name: String,
            duration: Duration? = null,
            classes: List<Class<*>> = emptyList(),
            key: K,
            block: (K) -> V
        ) = getOrComputeNullable(name, duration, classes, key, block)!!
    }
}