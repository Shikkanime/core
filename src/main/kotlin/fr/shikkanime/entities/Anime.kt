package fr.shikkanime.entities

import fr.shikkanime.entities.enums.CountryCode
import jakarta.persistence.*
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.FullTextField
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed
import java.time.ZonedDateTime
import java.util.*

@Entity
@Table(name = "anime")
@Indexed
data class Anime(
    override val uuid: UUID? = null,
    @Column(nullable = false, name = "country_code")
    @Enumerated(EnumType.STRING)
    @FullTextField
    val countryCode: CountryCode? = null,
    @Column(nullable = false)
    @FullTextField(analyzer = "shikkanime_analyzer")
    val name: String? = null,
    @Column(nullable = false, name = "release_date_time")
    val releaseDateTime: ZonedDateTime = ZonedDateTime.now(),
    @Column(nullable = false, columnDefinition = "VARCHAR(1000)")
    var image: String? = null,
    @Column(nullable = true, columnDefinition = "VARCHAR(2000)")
    var description: String? = null,
    @ManyToMany
    @JoinTable(
        name = "anime_simulcast",
        joinColumns = [JoinColumn(name = "anime_uuid")],
        inverseJoinColumns = [JoinColumn(name = "simulcast_uuid")]
    )
    var simulcasts: MutableSet<Simulcast> = mutableSetOf(),
) : ShikkEntity(uuid)
