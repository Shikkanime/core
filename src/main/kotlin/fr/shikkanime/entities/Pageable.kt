package fr.shikkanime.entities

data class Pageable<T>(
    val data: List<T>,
    val page: Int,
    val limit: Int,
    val total: Long,
)
