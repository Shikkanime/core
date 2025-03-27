package fr.shikkanime.entities

import fr.shikkanime.entities.enums.ImageType
import jakarta.persistence.*
import org.hibernate.annotations.Cache
import org.hibernate.annotations.CacheConcurrencyStrategy
import java.time.ZonedDateTime
import java.util.*

@Entity
@Table(
    name = "attachment",
    indexes = [
        Index(name = "attachment_entity_uuid_index", columnList = "entity_uuid"),
        Index(name = "attachment_type_index", columnList = "type"),
    ]
)
@Cacheable
@Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
class Attachment(
    uuid: UUID? = null,
    @Column(name = "creation_date_time", nullable = false)
    var creationDateTime: ZonedDateTime = ZonedDateTime.now(),
    @Column(name = "last_update_date_time", nullable = false)
    var lastUpdateDateTime: ZonedDateTime = ZonedDateTime.now(),
    @Column(name = "entity_uuid", nullable = false)
    var entityUuid: UUID? = null,
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    var type: ImageType? = null,
    @Column(length = 1000)
    var url: String? = null,
    @Column(nullable = false)
    var active: Boolean = true,
) : ShikkEntity(uuid)