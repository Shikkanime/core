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
    var name: String? = null,
    @Column(nullable = false, name = "release_date_time")
    var releaseDateTime: ZonedDateTime = ZonedDateTime.now(),
    @Column(nullable = false, columnDefinition = "VARCHAR(1000)")
    var image: String? = null,
    @Column(nullable = true, columnDefinition = "VARCHAR(1000)")
    var banner: String? = null,
    @Column(nullable = true, columnDefinition = "VARCHAR(2000)")
    var description: String? = null,
    @ManyToMany
    @JoinTable(
        name = "anime_simulcast",
        joinColumns = [JoinColumn(name = "anime_uuid")],
        inverseJoinColumns = [JoinColumn(name = "simulcast_uuid")]
    )
    var simulcasts: MutableSet<Simulcast> = mutableSetOf(),
    @Column(nullable = true)
    var slug: String? = null,
) : ShikkEntity(uuid) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Anime

        if (uuid != other.uuid) return false
        if (countryCode != other.countryCode) return false
        if (name != other.name) return false
        if (releaseDateTime != other.releaseDateTime) return false
        if (image != other.image) return false
        if (banner != other.banner) return false
        if (description != other.description) return false
        if (simulcasts != other.simulcasts) return false
        if (slug != other.slug) return false

        return true
    }

    override fun hashCode(): Int {
        var result = uuid?.hashCode() ?: 0
        result = 31 * result + (countryCode?.hashCode() ?: 0)
        result = 31 * result + (name?.hashCode() ?: 0)
        result = 31 * result + releaseDateTime.hashCode()
        result = 31 * result + (image?.hashCode() ?: 0)
        result = 31 * result + (banner?.hashCode() ?: 0)
        result = 31 * result + (description?.hashCode() ?: 0)
        result = 31 * result + simulcasts.hashCode()
        result = 31 * result + (slug?.hashCode() ?: 0)
        return result
    }
}
