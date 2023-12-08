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
    var url: String? = null,
    @Column(nullable = false)
    var image: String? = null
) : ShikkEntity(uuid)
