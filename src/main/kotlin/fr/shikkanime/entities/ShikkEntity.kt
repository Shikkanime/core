package fr.shikkanime.entities

import fr.shikkanime.modules.EntityLifecycleListener
import jakarta.persistence.EntityListeners
import jakarta.persistence.Id
import jakarta.persistence.MappedSuperclass
import org.hibernate.annotations.UuidGenerator
import java.io.Serializable
import java.util.*

@MappedSuperclass
@EntityListeners(EntityLifecycleListener::class)
open class ShikkEntity(
    @Id
    @UuidGenerator
    open val uuid: UUID?,
) : Serializable