package fr.shikkanime.entities

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table
import java.util.*

@Entity
@Table(name = "platform")
data class Platform(
    override val uuid: UUID? = null,
    @Column(nullable = false, unique = true)
    val name: String? = null,
    @Column(nullable = false)
    val url: String? = null,
    @Column(nullable = false)
    val image: String? = null
) : ShikkEntity(uuid) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Platform

        if (uuid != other.uuid) return false
        if (name != other.name) return false
        if (url != other.url) return false
        if (image != other.image) return false

        return true
    }

    override fun hashCode(): Int {
        var result = uuid?.hashCode() ?: 0
        result = 31 * result + (name?.hashCode() ?: 0)
        result = 31 * result + (url?.hashCode() ?: 0)
        result = 31 * result + (image?.hashCode() ?: 0)
        return result
    }

    override fun toString(): String {
        return "Platform(uuid=$uuid, name=$name, url=$url, image=$image)"
    }
}
