package fr.shikkanime.entities

data class Pageable<T>(
    var data: Set<T>,
    val page: Int,
    val limit: Int,
    val total: Long,
)
