package fr.shikkanime.entities

import jakarta.persistence.*
import java.time.ZonedDateTime
import java.util.*

@Entity
@Table(name = "anime")
data class Anime(
    override val uuid: UUID? = null,
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    val country: Country? = null,
    @Column(nullable = false)
    val name: String? = null,
    @Column(nullable = false, name = "release_date")
    val releaseDate: ZonedDateTime = ZonedDateTime.now(),
    @Column(nullable = false)
    var image: String? = null,
    @Column(nullable = true, columnDefinition = "TEXT")
    var description: String? = null,
) : ShikkEntity(uuid)
