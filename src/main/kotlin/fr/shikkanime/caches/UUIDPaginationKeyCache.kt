package fr.shikkanime.caches

import java.util.*

data class UUIDPaginationKeyCache(
    val uuid: UUID,
    val page: Int,
    val limit: Int,
)
