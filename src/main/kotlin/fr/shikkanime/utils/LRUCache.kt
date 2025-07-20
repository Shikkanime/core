package fr.shikkanime.utils

import java.util.*

/**
 * A simple LRU (Least Recently Used) cache implementation using LinkedHashMap.
 * This cache has a maximum size and will remove the least recently accessed items
 * when the size exceeds the maximum.
 *
 * @param K the type of keys in this cache
 * @param V the type of values in this cache
 * @param maxSize the maximum number of entries the cache can hold
 */
class LRUCache<K, V>(private val maxSize: Int) {
    private val cache = Collections.synchronizedMap(object : LinkedHashMap<K, V>(maxSize, 0.75f, true) {
        override fun removeEldestEntry(eldest: Map.Entry<K, V>): Boolean {
            return size > maxSize
        }
    })

    fun containsKey(key: K): Boolean {
        return cache.containsKey(key)
    }

    /**
     * Puts a value into the cache with the specified key.
     * If the cache is full, the least recently used item will be removed.
     *
     * @param key the key with which the specified value is to be associated
     * @param value the value to be associated with the specified key
     * @return the previous value associated with the key, or null if there was no mapping
     */
    operator fun set(key: K, value: V): V? {
        return cache.put(key, value)
    }

    /**
     * Returns the value associated with the specified key, or null if there is no mapping.
     * This operation will also mark the entry as recently used.
     *
     * @param key the key whose associated value is to be returned
     * @return the value associated with the key, or null if no mapping exists
     */
    operator fun get(key: K): V? {
        return cache[key]
    }

    fun remove(key: K): V? {
        return cache.remove(key)
    }

    /**
     * Removes all entries from the cache.
     */
    fun clear() {
        cache.clear()
    }

    /**
     * Returns the number of entries in the cache.
     *
     * @return the number of entries in the cache
     */
    fun size(): Int {
        return cache.size
    }
} 