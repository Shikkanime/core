package fr.shikkanime.utils.strategies

import com.google.gson.reflect.TypeToken
import fr.shikkanime.utils.SerializationUtils
import fr.shikkanime.utils.SynchronizedGlideClient

class ValkeyCacheStrategy<K, V>(
    val name: String,
    val serializationType: SerializationUtils.SerializationType,
    val typeToken: TypeToken<V>
) : ICacheStrategy<K, V> {
    private fun buildMapKey(key: K): String {
        val keyString = key.toString()
        return "$name${if (keyString.isNotBlank()) ":$keyString" else ""}"
    }

    private fun keys(): List<String> {
        return SynchronizedGlideClient.search("$name*").filter {
            val substringAfter = it.substringAfter(name)
            substringAfter.isBlank() || substringAfter.startsWith(":")
        }
    }

    private fun getCachedValue(key: String): String? = SynchronizedGlideClient[key]

    override fun containsKey(key: K) = getCachedValue(buildMapKey(key)) != null

    override fun get(key: K) = getCachedValue(buildMapKey(key))?.let { SerializationUtils.deserialize(serializationType, it, typeToken) }

    override fun put(key: K, value: V) {
        SynchronizedGlideClient[buildMapKey(key)] = SerializationUtils.serialize(serializationType, value)
    }

    override fun putIfNotExists(key: K, value: V) {
        if (!containsKey(key))
            put(key, value)
    }

    override fun remove(key: K) {
        SynchronizedGlideClient.del(arrayOf(buildMapKey(key)))
    }

    override fun clear() {
        val keys = keys()

        if (keys.isNotEmpty())
            SynchronizedGlideClient.del(keys.toTypedArray())
    }
}