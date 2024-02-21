package fr.shikkanime.entities

import jakarta.persistence.Cacheable
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table
import org.hibernate.annotations.Cache
import org.hibernate.annotations.CacheConcurrencyStrategy
import java.util.*

@Entity
@Table(name = "simulcast")
@Cacheable
@Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
class Simulcast(
    override val uuid: UUID? = null,
    @Column(nullable = false)
    val season: String? = null,
    @Column(nullable = false, name = "year_")
    val year: Int? = null,
) : ShikkEntity(uuid)
