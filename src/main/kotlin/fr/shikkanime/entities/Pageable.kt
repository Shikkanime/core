package fr.shikkanime.entities

data class Pageable<T>(
    var data: List<T>,
    val page: Int,
    val limit: Int,
    val total: Long,
)
