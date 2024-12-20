package fr.shikkanime.utils

import com.google.common.cache.CacheBuilder
import com.google.common.cache.CacheLoader
import com.google.common.cache.LoadingCache
import java.time.Duration
import kotlin.time.measureTimedValue

class MapCache<K : Any, V>(
    private val name: String,
    private var duration: Duration? = null,
    private val classes: List<Class<*>> = emptyList(),
    private val fn: () -> List<K> = { emptyList() },
    private val requiredCaches: () -> List<MapCache<*, *>> = { emptyList() },
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

    private fun loadDefaultKeys(): Boolean {
        val fn = fn()
        fn.forEach { this[it] }
        return fn.isNotEmpty()
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

    fun invalidate(): Boolean {
        cache.invalidateAll()
        return loadDefaultKeys()
    }

    companion object {
        private val logger = LoggerFactory.getLogger(this::class.java)
        private val globalCaches: MutableList<MapCache<*, *>> = mutableListOf()

        fun loadAll() {
            val loadedCaches = mutableSetOf<MapCache<*, *>>()

            globalCaches
                .sortedBy { it.requiredCaches().size }
                .flatMap { it.requiredCaches() + it }
                .filter { loadedCaches.add(it) }
                .forEach {
                    val take = measureTimedValue { it.loadDefaultKeys() }

                    if (take.value)
                        logger.info("Cache ${it.name} loaded in ${take.duration.inWholeMilliseconds} ms")
                }
        }

        @Synchronized
        fun invalidate(vararg classes: Class<*>) {
            val loadedCaches = mutableSetOf<MapCache<*, *>>()

            globalCaches.sortedBy { it.requiredCaches().size }
                .flatMap { it.requiredCaches() + it }
                .filter { it.classes.any { clazz -> classes.contains(clazz) } }
                .filter { loadedCaches.add(it) }
                .forEach {
                    val take = measureTimedValue { it.invalidate() }

                    if (take.value)
                        logger.info("Cache ${it.name} invalidated in ${take.duration.inWholeMilliseconds} ms")
                }
        }

        // For test only
        @Synchronized
        fun invalidateAll() {
            val loadedCaches = mutableSetOf<MapCache<*, *>>()

            globalCaches.sortedBy { it.requiredCaches().size }
                .flatMap { it.requiredCaches() + it }
                .filter { loadedCaches.add(it) }
                .forEach {
                    val take = measureTimedValue { it.invalidate() }

                    if (take.value)
                        logger.info("Cache ${it.name} invalidated in ${take.duration.inWholeMilliseconds} ms")
                }
        }
    }
}