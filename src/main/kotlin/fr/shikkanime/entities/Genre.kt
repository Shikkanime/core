package fr.shikkanime.entities

import jakarta.persistence.*
import org.hibernate.annotations.Cache
import org.hibernate.annotations.CacheConcurrencyStrategy
import java.util.*

@Entity
@Table(
    name = "genre",
    indexes = [Index(name = "idx_genre_name", columnList = "name", unique = true)]
)
@Cacheable
@Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
class Genre(
    uuid: UUID? = null,
    @Column(nullable = false)
    var name: String? = null,
) : ShikkEntity(uuid)
