package fr.shikkanime.entities

import jakarta.persistence.Cacheable
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table
import org.hibernate.annotations.Cache
import org.hibernate.annotations.CacheConcurrencyStrategy
import java.util.*

@Entity
@Table(name = "config")
@Cacheable
@Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
class Config(
    override val uuid: UUID? = null,
    @Column(nullable = false, name = "property_key", unique = true)
    val propertyKey: String? = null,
    @Column(nullable = false, name = "property_value")
    var propertyValue: String? = null,
) : ShikkEntity(uuid)
