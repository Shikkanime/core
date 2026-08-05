package fr.shikkanime.models

import kotlinx.datetime.LocalDateTime
import kotlin.uuid.Uuid

open class ShikkModel(
    open val id: Uuid,
    open val createdAt: LocalDateTime,
    open val updatedAt: LocalDateTime,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ShikkModel) return false
        if (id != other.id) return false
        return true
    }

    override fun hashCode(): Int =
        id.hashCode()

    override fun toString(): String =
        "ShikkModel(id=$id)"
}
