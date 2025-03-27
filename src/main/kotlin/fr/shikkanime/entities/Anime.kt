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
        Index(name = "idx_anime_country_code_slug", columnList = "country_code, slug", unique = true)
    ]
)
@Indexed
@Cacheable
@Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
class Anime(
    uuid: UUID? = null,
    @Column(nullable = false, name = "country_code")
    @Enumerated(EnumType.STRING)
    val countryCode: CountryCode? = null,
    @Column(nullable = false)
    @FullTextField(analyzer = "shikkanime_analyzer")
    var name: String? = null,
    @Column(nullable = false, name = "release_date_time")
    var releaseDateTime: ZonedDateTime = ZonedDateTime.now(),
    @Column(nullable = false, name = "last_release_date_time")
    var lastReleaseDateTime: ZonedDateTime = releaseDateTime,
    @Column(nullable = true, name = "last_update_date_time")
    var lastUpdateDateTime: ZonedDateTime? = releaseDateTime,
    @Column(nullable = true, columnDefinition = "VARCHAR(2000)")
    var description: String? = null,
    @Column(nullable = false)
    var slug: String? = null,
    // -----------------------------------------------------------------
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "anime_simulcast",
        joinColumns = [JoinColumn(name = "anime_uuid")],
        inverseJoinColumns = [JoinColumn(name = "simulcast_uuid")]
    )
    @Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
    var simulcasts: MutableSet<Simulcast> = mutableSetOf(),
    @OneToMany(mappedBy = "anime", fetch = FetchType.LAZY)
    @Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
    var platformIds: MutableSet<AnimePlatform> = mutableSetOf(),
    @OneToMany(mappedBy = "anime", fetch = FetchType.LAZY)
    @Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
    var mappings: MutableSet<EpisodeMapping> = mutableSetOf(),
    @OneToMany(mappedBy = "anime", fetch = FetchType.LAZY)
    @Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
    var followings: MutableSet<MemberFollowAnime> = mutableSetOf(),
) : ShikkEntity(uuid)
