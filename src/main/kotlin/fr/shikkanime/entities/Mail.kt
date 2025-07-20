package fr.shikkanime.entities

import jakarta.persistence.Cacheable
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table
import org.hibernate.annotations.Cache
import org.hibernate.annotations.CacheConcurrencyStrategy
import java.time.ZonedDateTime
import java.util.*

@Entity
@Table(name = "mail")
@Cacheable
@Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
class Mail(
    uuid: UUID? = null,
    @Column(name = "creation_date_time", nullable = false)
    var creationDateTime: ZonedDateTime = ZonedDateTime.now(),
    @Column(name = "last_update_date_time", nullable = true)
    var lastUpdateDateTime: ZonedDateTime? = null,
    @Column(name = "recipient", nullable = false)
    var recipient: String? = null,
    @Column(name = "title", nullable = false)
    var title: String? = null,
    @Column(name = "body", nullable = false, length = 10000)
    var body: String? = null,
    @Column(name = "sent", nullable = false)
    var sent: Boolean = false,
    @Column(name = "error", nullable = true, length = 10000)
    var error: String? = null
) : ShikkEntity(uuid)