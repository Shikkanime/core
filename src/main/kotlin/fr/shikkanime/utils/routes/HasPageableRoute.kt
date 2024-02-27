package fr.shikkanime.utils.routes

import fr.shikkanime.entities.SortParameter

open class HasPageableRoute {
    protected fun pageableRoute(
        pageParam: Int?,
        limitParam: Int?,
        sortParam: String?,
        descParam: String?
    ): Triple<Int, Int, List<SortParameter>> {
        val page = pageParam ?: 1
        val limit = limitParam?.coerceIn(1, 30) ?: 15

        val sortParameters = sortParam?.split(",")?.map { sort ->
            val desc = descParam?.split(",")?.contains(sort) ?: false
            SortParameter(sort, if (desc) SortParameter.Order.DESC else SortParameter.Order.ASC)
        } ?: mutableListOf()

        return Triple(page, limit, sortParameters)
    }
}