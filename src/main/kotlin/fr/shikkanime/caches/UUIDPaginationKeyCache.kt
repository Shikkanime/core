package fr.shikkanime.caches

import java.util.*

open class UUIDPaginationKeyCache(
    open val uuid: UUID,
    open val page: Int,
    open val limit: Int,
)
