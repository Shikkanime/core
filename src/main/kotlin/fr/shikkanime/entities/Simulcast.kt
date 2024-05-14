package fr.shikkanime.entities

import jakarta.persistence.*
import org.hibernate.annotations.Cache
import org.hibernate.annotations.CacheConcurrencyStrategy
import java.util.*

@Entity
@Table(
    name = "simulcast",
    indexes = [
        Index(name = "idx_simulcast_season", columnList = "season"),
        Index(name = "idx_simulcast_year", columnList = "year_"),
        Index(name = "idx_simulcast_season_year", columnList = "season, year_"),
    ]
)
@Cacheable
@Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
class Simulcast(
    uuid: UUID? = null,
    @Column(nullable = false)
    val season: String? = null,
    @Column(nullable = false, name = "year_")
    val year: Int? = null,
) : ShikkEntity(uuid)
