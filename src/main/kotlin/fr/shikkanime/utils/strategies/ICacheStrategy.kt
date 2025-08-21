package fr.shikkanime.utils.strategies

interface ICacheStrategy<K, V> {
    fun containsKey(key: K): Boolean
    operator fun get(key: K): V?
    fun put(key: K, value: V)
    fun putIfNotExists(key: K, value: V)
    fun remove(key: K)
    fun clear()
}