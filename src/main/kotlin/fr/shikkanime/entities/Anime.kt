package fr.shikkanime.entities

import fr.shikkanime.entities.enums.CountryCode
import jakarta.persistence.*
import java.time.ZonedDateTime
import java.util.*

@Entity
@Table(name = "anime")
data class Anime(
    override val uuid: UUID? = null,
    @Column(nullable = false, name = "country_code")
    @Enumerated(EnumType.STRING)
    val countryCode: CountryCode? = null,
    @Column(nullable = false)
    val name: String? = null,
    @Column(nullable = false, name = "release_date")
    val releaseDate: ZonedDateTime = ZonedDateTime.now(),
    @Column(nullable = false, columnDefinition = "VARCHAR(1000)")
    var image: String? = null,
    @Column(nullable = true, columnDefinition = "VARCHAR(1000)")
    var description: String? = null,
) : ShikkEntity(uuid)
