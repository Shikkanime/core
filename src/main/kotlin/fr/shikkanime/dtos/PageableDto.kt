package fr.shikkanime.dtos

import fr.shikkanime.entities.miscellaneous.Pageable
import fr.shikkanime.factories.IGenericFactory

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

        inline fun <T : Any, reified D> fromPageable(pageable: Pageable<T>, factory: IGenericFactory<T, D>): PageableDto<D> {
            return PageableDto(
                data = pageable.data.map { factory.toDto(it) }.toSet(),
                page = pageable.page,
                limit = pageable.limit,
                total = pageable.total,
            )
        }

        inline fun <T : Any, reified R> fromPageable(pageable: Pageable<T>, fn: (T) -> R): PageableDto<R> {
            return PageableDto(
                data = pageable.data.map(fn).toSet(),
                page = pageable.page,
                limit = pageable.limit,
                total = pageable.total,
            )
        }
    }
}
