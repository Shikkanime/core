package fr.shikkanime.utils.strategies

interface ICacheStrategy<K, V> {
    operator fun get(key: K): V?
    fun put(key: K, value: V)
    fun remove(key: K)
    fun clear()
}