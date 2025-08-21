package fr.shikkanime.entities

import jakarta.persistence.Id
import jakarta.persistence.MappedSuperclass
import org.hibernate.annotations.UuidGenerator
import java.io.Serializable
import java.util.*

@MappedSuperclass
open class ShikkEntity(
    @Id
    @UuidGenerator
    open val uuid: UUID?,
) : Serializable