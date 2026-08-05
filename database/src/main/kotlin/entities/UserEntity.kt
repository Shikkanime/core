package fr.shikkanime.database.entities

import fr.shikkanime.database.enumSet
import fr.shikkanime.models.UserRole
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.crypt.Argon2Hasher
import org.jetbrains.exposed.v1.crypt.hashed
import org.jetbrains.exposed.v1.dao.UuidEntityClass
import kotlin.uuid.Uuid

const val USER_TABLE_NAME = "user"
const val USER_TABLE_ID = USER_TABLE_NAME + ID

object UserTable : ShikkTable(USER_TABLE_NAME) {
    val identifier = binary("identifier").uniqueIndex("identifier$UQ").nullable()
    val username = varchar("username", 255).uniqueIndex("username$UQ").nullable()
    val password = text("password").hashed(Argon2Hasher(memory = 65_536, iterations = 3)).nullable()
    val roles = enumSet<UserRole>("roles").nullable()
}

class UserEntity(id: EntityID<Uuid>) : ShikkEntity(id, UserTable) {
    companion object : UuidEntityClass<UserEntity>(UserTable)

    var identifier by UserTable.identifier
    var username by UserTable.username
    var password by UserTable.password
    var roles by UserTable.roles
}