package fr.shikkanime.database.repositories

import fr.shikkanime.database.entities.UserEntity
import fr.shikkanime.exposed.repositories.EntityRepository
import fr.shikkanime.models.UserRole
import kotlin.uuid.Uuid

abstract class UserRepository : EntityRepository<Uuid, UserEntity>(UserEntity) {
    abstract fun findAllByRoles(vararg roles: UserRole): List<UserEntity>

    abstract fun findByIdentifier(identifier: ByteArray): UserEntity?

    abstract fun findByUsername(username: String): UserEntity?
}