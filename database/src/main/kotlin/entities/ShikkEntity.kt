package fr.shikkanime.database.entities

import fr.shikkanime.core.TimeUtils
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.UuidTable
import org.jetbrains.exposed.v1.dao.UuidEntity
import org.jetbrains.exposed.v1.datetime.datetime
import kotlin.uuid.Uuid

/**
 * Base table carrying the audit columns every persisted row owns.
 *
 * The timestamps are computed client-side, never by a database default: Exposed re-reads a row it
 * has just inserted when one of its columns is filled by the database, which costs one extra
 * `SELECT` per insert. A client default is written by the same `INSERT`, so the row is complete as
 * soon as it is written.
 */
open class ShikkTable(name: String) : UuidTable(name) {
    val createdAt = datetime(CREATED_AT)
        .clientDefault { TimeUtils.now() }
    val updatedAt = datetime(UPDATED_AT)
        .clientDefault { TimeUtils.now() }
}

open class ShikkEntity(
    id: EntityID<Uuid>,
    table: ShikkTable
) : UuidEntity(id) {
    var createdAt by table.createdAt
    var updatedAt by table.updatedAt
}