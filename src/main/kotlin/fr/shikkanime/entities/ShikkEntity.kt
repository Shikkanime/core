package fr.shikkanime.entities

import jakarta.persistence.Id
import jakarta.persistence.MappedSuperclass
import org.hibernate.annotations.UuidGenerator
import java.io.Serializable
import java.util.*

@MappedSuperclass
open class ShikkEntity(
    @Id
    @UuidGenerator
    open val uuid: UUID? = null,
) : Serializable {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ShikkEntity) return false

        if (uuid != other.uuid) return false

        return true
    }

    override fun hashCode(): Int {
        return uuid?.hashCode() ?: 0
    }

    override fun toString(): String {
        return "ShikkEntity(uuid=$uuid)"
    }
}
