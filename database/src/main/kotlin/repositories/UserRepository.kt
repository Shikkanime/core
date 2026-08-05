package fr.shikkanime.database.repositories

import fr.shikkanime.database.entities.UserEntity
import fr.shikkanime.exposed.repositories.AbstractRepository
import fr.shikkanime.models.UserRole
import kotlin.uuid.Uuid

abstract class UserRepository : AbstractRepository<Uuid, UserEntity>(UserEntity) {
    abstract fun findAllByRoles(vararg roles: UserRole): List<UserEntity>

    abstract fun findByIdentifier(identifier: ByteArray): UserEntity?

    abstract fun findByUsername(username: String): UserEntity?
}