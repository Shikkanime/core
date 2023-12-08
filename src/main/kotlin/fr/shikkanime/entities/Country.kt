package fr.shikkanime.entities

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table
import java.util.*

@Entity
@Table(name = "country")
data class Country(
    override val uuid: UUID? = null,
    @Column(nullable = false, unique = true)
    val name: String? = null,
    @Column(nullable = false, unique = true, name = "country_code")
    val countryCode: String? = null,
) : ShikkEntity(uuid)