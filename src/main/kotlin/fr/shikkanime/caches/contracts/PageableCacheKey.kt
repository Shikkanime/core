package fr.shikkanime.caches.contracts

interface PageableCacheKey {
    val page: Int
    val limit: Int
}
