package fr.shikkanime.database.entities

import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.UuidTable
import org.jetbrains.exposed.v1.dao.UuidEntity
import org.jetbrains.exposed.v1.datetime.CurrentDateTime
import org.jetbrains.exposed.v1.datetime.datetime
import kotlin.uuid.Uuid

open class ShikkTable(name: String) : UuidTable(name) {
    val createdAt = datetime(CREATED_AT).defaultExpression(CurrentDateTime)
    val updatedAt = datetime(UPDATED_AT).defaultExpression(CurrentDateTime)
}

open class ShikkEntity(
    id: EntityID<Uuid>,
    table: ShikkTable
) : UuidEntity(id) {
    var createdAt by table.createdAt
    var updatedAt by table.updatedAt
}