package fr.shikkanime.entities

import jakarta.persistence.*
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
