package fr.shikkanime.entities

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Index
import jakarta.persistence.Table
import java.util.*

@Entity
@Table(
    name = "country",
    indexes = [
        Index(name = "idx_country_country_code", columnList = "country_code")
    ]
)
data class Country(
    override val uuid: UUID? = null,
    @Column(nullable = false, unique = true)
    val name: String? = null,
    @Column(nullable = false, unique = true, name = "country_code")
    val countryCode: String? = null,
) : ShikkEntity(uuid) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Country) return false
        if (!super.equals(other)) return false

        if (uuid != other.uuid) return false
        if (name != other.name) return false
        if (countryCode != other.countryCode) return false

        return true
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + (uuid?.hashCode() ?: 0)
        result = 31 * result + (name?.hashCode() ?: 0)
        result = 31 * result + (countryCode?.hashCode() ?: 0)
        return result
    }

    override fun toString(): String {
        return "Country(uuid=$uuid, name=$name, countryCode=$countryCode)"
    }
}
