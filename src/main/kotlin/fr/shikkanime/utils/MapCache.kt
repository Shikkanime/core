package fr.shikkanime.utils

import com.google.gson.reflect.TypeToken
import fr.shikkanime.utils.strategies.InMemoryCacheStrategy
import fr.shikkanime.utils.strategies.ValkeyCacheStrategy
import kotlinx.coroutines.runBlocking
import java.io.Serializable
import java.time.Duration

private val defaultCacheDuration: Duration = Duration.ofDays(1)
private val defaultSerializationType = SerializationUtils.SerializationType.JSON

data class MapCacheValue<V : Serializable>(
    val timestamp: Long,
    val value: V
) : Serializable

class MapCache<K : Any, V : Serializable>(
    name: String,
    private var duration: Duration = defaultCacheDuration,
    classes: List<Class<*>> = emptyList(),
    typeToken: TypeToken<MapCacheValue<V>>,
    serializationType: SerializationUtils.SerializationType = defaultSerializationType,
    private val block: suspend (K) -> V?,
) : InvalidationService(classes) {
    private val cacheStrategy = if (!AsynchronizedGlideClient.isAvailable()) InMemoryCacheStrategy<K, MapCacheValue<V>>() else ValkeyCacheStrategy<K, MapCacheValue<V>>(name, serializationType, typeToken)

    init {
        register(name, this)
    }

    fun containsKey(key: K) = cacheStrategy.containsKey(key)

    suspend operator fun get(key: K): V? {
        val cachedValue = cacheStrategy[key]

        if (cachedValue == null || System.currentTimeMillis() - cachedValue.timestamp > duration.toMillis()) {
            val newValue = block(key)

            if (newValue != null)
                cacheStrategy.put(key, MapCacheValue(System.currentTimeMillis(), newValue))
            else
                cacheStrategy.remove(key)

            return newValue
        }

        return cachedValue.value
    }

    fun putIfNotExists(key: K, value: V) {
        cacheStrategy.putIfNotExists(key, MapCacheValue(System.currentTimeMillis(), value))
    }

    override fun invalidate() {
        cacheStrategy.clear()
    }

    companion object {
        fun <K : Any, V : Serializable> getOrComputeNullable(
            name: String,
            duration: Duration = defaultCacheDuration,
            classes: List<Class<*>> = emptyList(),
            typeToken: TypeToken<MapCacheValue<V>>,
            serializationType: SerializationUtils.SerializationType = defaultSerializationType,
            key: K,
            block: (K) -> V?
        ): V? {
            @Suppress("UNCHECKED_CAST")
            val mapCache = getByNameAndType(name, MapCache::class) as? MapCache<K, V> ?: MapCache(name, duration, classes, typeToken, serializationType) { runBlocking { block(it) } }
            return runBlocking { mapCache[key] }
        }

        fun <K : Any, V : Serializable> getOrCompute(
            name: String,
            duration: Duration = defaultCacheDuration,
            classes: List<Class<*>> = emptyList(),
            typeToken: TypeToken<MapCacheValue<V>>,
            serializationType: SerializationUtils.SerializationType = defaultSerializationType,
            key: K,
            block: (K) -> V
        ) = getOrComputeNullable(name, duration, classes, typeToken, serializationType, key, block)!!

        suspend fun <K : Any, V : Serializable> getOrComputeNullableAsync(
            name: String,
            duration: Duration = defaultCacheDuration,
            classes: List<Class<*>> = emptyList(),
            typeToken: TypeToken<MapCacheValue<V>>,
            serializationType: SerializationUtils.SerializationType = defaultSerializationType,
            key: K,
            block: suspend (K) -> V?
        ): V? {
            @Suppress("UNCHECKED_CAST")
            val mapCache = getByNameAndType(name, MapCache::class) as? MapCache<K, V> ?: MapCache(name, duration, classes, typeToken, serializationType, block)
            return mapCache[key]
        }

        suspend fun <K : Any, V : Serializable> getOrComputeAsync(
            name: String,
            duration: Duration = defaultCacheDuration,
            classes: List<Class<*>> = emptyList(),
            typeToken: TypeToken<MapCacheValue<V>>,
            serializationType: SerializationUtils.SerializationType = defaultSerializationType,
            key: K,
            block: suspend (K) -> V
        ) = getOrComputeNullableAsync(name, duration, classes, typeToken, serializationType, key, block)!!
    }
}