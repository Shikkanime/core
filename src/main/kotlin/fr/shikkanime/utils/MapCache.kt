package fr.shikkanime.utils

import java.time.Duration
import java.time.ZonedDateTime

class MapCache<K : Any, V>(
    private var duration: Duration? = null,
    private val classes: List<Class<*>> = listOf(),
    private val block: (K) -> V,
) {
    private val cache = mutableMapOf<K, V>()
    private var lastInvalidation: ZonedDateTime? = null

    init {
        globalCaches.add(this)
    }

    private fun getUnchecked(key: K): V? {
        if (lastInvalidation != null && duration != null && lastInvalidation!!.plus(duration) < ZonedDateTime.now()) {
            invalidate()
        }

        return cache[key] ?: kotlin.run {
            logger.info("Loading $key")
            val value = block(key)
            cache[key] = value
            lastInvalidation = ZonedDateTime.now()
            value
        }
    }

    operator fun get(key: K): V? {
        return try {
            getUnchecked(key)
        } catch (e: Exception) {
            null
        }
    }

    operator fun set(key: K, value: V & Any) {
        cache[key] = value
    }

    fun resetWithNewDuration(duration: Duration) {
        this.duration = duration
        invalidate()
    }

    fun invalidate() {
        cache.clear()
        lastInvalidation = ZonedDateTime.now()
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