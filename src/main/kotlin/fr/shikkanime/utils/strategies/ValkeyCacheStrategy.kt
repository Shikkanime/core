package fr.shikkanime.utils.strategies

import com.google.gson.reflect.TypeToken
import fr.shikkanime.utils.AsynchronizedGlideClient
import fr.shikkanime.utils.SerializationUtils

class ValkeyCacheStrategy<K, V>(
    val name: String,
    val serializationType: SerializationUtils.SerializationType,
    val typeToken: TypeToken<V>,
    private val localCacheMaxSize: Int = DEFAULT_LOCAL_CACHE_SIZE
) : ICacheStrategy<K, V> {
    companion object {
        const val DEFAULT_LOCAL_CACHE_SIZE = 100
    }

    private val localCache: MutableMap<K, V> = object : LinkedHashMap<K, V>(16, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<K, V>): Boolean {
            return size > localCacheMaxSize
        }
    }

    private fun buildMapKey(key: K): String {
        val keyString = key.toString()
        return "$name${if (keyString.isNotBlank()) ":$keyString" else ""}"
    }

    private fun keys(): List<String> {
        return AsynchronizedGlideClient.searchAll("$name*").filter {
            val substringAfter = it.substringAfter(name)
            substringAfter.isBlank() || substringAfter.startsWith(":")
        }
    }

    private fun getCachedValue(key: String): String? = AsynchronizedGlideClient[key]

    override fun containsKey(key: K): Boolean {
        synchronized(localCache) {
            if (localCache.containsKey(key)) return true
        }

        return getCachedValue(buildMapKey(key)) != null
    }

    override fun get(key: K): V? {
        synchronized(localCache) {
            localCache[key]?.let { return it }
        }

        return getCachedValue(buildMapKey(key))?.let {
            val deserialized = SerializationUtils.deserialize(serializationType, it, typeToken)

            if (deserialized != null)
                synchronized(localCache) { localCache[key] = deserialized }

            deserialized
        }
    }

    override fun put(key: K, value: V) {
        synchronized(localCache) { localCache[key] = value }
        AsynchronizedGlideClient[buildMapKey(key)] = SerializationUtils.serialize(serializationType, value)
    }

    override fun putIfNotExists(key: K, value: V) {
        if (!containsKey(key))
            put(key, value)
    }

    override fun remove(key: K) {
        synchronized(localCache) { localCache.remove(key) }
        AsynchronizedGlideClient.del(arrayOf(buildMapKey(key)))
    }

    override fun clear() {
        synchronized(localCache) { localCache.clear() }
        val keys = keys()

        if (keys.isNotEmpty())
            AsynchronizedGlideClient.delInPool(keys)
    }
}