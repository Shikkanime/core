package fr.shikkanime.entities

import fr.shikkanime.entities.enums.CountryCode
import jakarta.persistence.*
import org.hibernate.annotations.Cache
import org.hibernate.annotations.CacheConcurrencyStrategy
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.FullTextField
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed
import java.time.ZonedDateTime
import java.util.*

@Entity
@Table(
    name = "anime",
    indexes = [
        Index(name = "idx_anime_country_code", columnList = "country_code"),
    ]
)
@Indexed
@Cacheable
@Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
class Anime(
    override val uuid: UUID? = null,
    @Column(nullable = false, name = "country_code")
    @Enumerated(EnumType.STRING)
    @FullTextField
    val countryCode: CountryCode? = null,
    @Column(nullable = false)
    @FullTextField(analyzer = "shikkanime_analyzer")
    var name: String? = null,
    @Column(nullable = false, name = "release_date_time")
    var releaseDateTime: ZonedDateTime = ZonedDateTime.now(),
    @Column(nullable = false, columnDefinition = "VARCHAR(1000)")
    var image: String? = null,
    @Column(nullable = true, columnDefinition = "VARCHAR(1000)")
    var banner: String? = null,
    @Column(nullable = true, columnDefinition = "VARCHAR(2000)")
    var description: String? = null,
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "anime_simulcast",
        joinColumns = [JoinColumn(name = "anime_uuid")],
        inverseJoinColumns = [JoinColumn(name = "simulcast_uuid")]
    )
    @Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
    var simulcasts: MutableSet<Simulcast> = mutableSetOf(),
    @Column(nullable = true)
    var slug: String? = null,
    @Column(nullable = false, name = "last_release_date_time")
    var lastReleaseDateTime: ZonedDateTime = releaseDateTime,
) : ShikkEntity(uuid) {
    fun copy() = Anime(
        uuid,
        countryCode,
        name,
        releaseDateTime,
        image,
        banner,
        description,
        simulcasts,
        slug,
        lastReleaseDateTime
    )
}
