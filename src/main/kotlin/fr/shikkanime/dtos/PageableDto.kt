package fr.shikkanime.dtos

import fr.shikkanime.converters.AbstractConverter
import fr.shikkanime.entities.Pageable

data class PageableDto<T>(
    val data: List<T>,
    val page: Int,
    val limit: Int,
    val total: Long,
) {
    companion object {
        fun <T> empty(): PageableDto<T> {
            return PageableDto(
                data = emptyList(),
                page = 0,
                limit = 0,
                total = 0,
            )
        }

        inline fun <T : Any, reified D> fromPageable(pageable: Pageable<T>, dtoClass: Class<D>): PageableDto<D> {
            return PageableDto(
                data = AbstractConverter.convert(pageable.data, dtoClass)!!,
                page = pageable.page,
                limit = pageable.limit,
                total = pageable.total,
            )
        }
    }
}
