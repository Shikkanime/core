package fr.shikkanime.utils.strategies

import io.ktor.util.collections.*

class InMemoryCacheStrategy<K, V> : ICacheStrategy<K, V> {
    private val cache: ConcurrentMap<K, V> = ConcurrentMap()

    override fun get(key: K) = cache[key]

    override fun put(key: K, value: V) {
        cache[key] = value
    }

    override fun remove(key: K) {
        cache.remove(key)
    }

    override fun clear() {
        cache.clear()
    }
}