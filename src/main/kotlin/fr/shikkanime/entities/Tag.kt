package fr.shikkanime.entities

import jakarta.persistence.Cacheable
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Index
import jakarta.persistence.Table
import org.hibernate.annotations.Cache
import org.hibernate.annotations.CacheConcurrencyStrategy
import java.util.*

@Entity
@Table(
    name = "tag",
    indexes = [Index(name = "idx_tag_name", columnList = "name", unique = true)]
)
@Cacheable
@Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
class Tag(
    uuid: UUID? = null,
    @Column(nullable = false)
    var name: String? = null,
) : ShikkEntity(uuid)
