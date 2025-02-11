package fr.shikkanime.dtos

import fr.shikkanime.converters.AbstractConverter
import fr.shikkanime.entities.miscellaneous.Pageable

data class PageableDto<T>(
    val data: Set<T>,
    val page: Int,
    val limit: Int,
    val total: Long,
) {
    companion object {
        fun <T> empty(): PageableDto<T> {
            return PageableDto(
                data = emptySet(),
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

        fun <T : Any, R> fromPageable(pageable: Pageable<T>, fn: (T) -> R): PageableDto<R> {
            return PageableDto(
                data = pageable.data.map(fn).toSet(),
                page = pageable.page,
                limit = pageable.limit,
                total = pageable.total,
            )
        }
    }
}
