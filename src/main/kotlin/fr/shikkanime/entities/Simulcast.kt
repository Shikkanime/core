package fr.shikkanime.entities

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table
import java.util.*

@Entity
@Table(name = "simulcast")
data class Simulcast(
    override val uuid: UUID? = null,
    @Column(nullable = false)
    val season: String? = null,
    @Column(nullable = false)
    val year: Int? = null,
) : ShikkEntity(uuid) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Simulcast) return false

        if (uuid != other.uuid) return false
        if (season != other.season) return false
        if (year != other.year) return false

        return true
    }

    override fun hashCode(): Int {
        var result = uuid?.hashCode() ?: 0
        result = 31 * result + (season?.hashCode() ?: 0)
        result = 31 * result + (year ?: 0)
        return result
    }
}
