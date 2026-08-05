package fr.shikkanime.database.repositories.impl

import fr.shikkanime.database.entities.UserEntity
import fr.shikkanime.database.entities.UserTable
import fr.shikkanime.database.repositories.UserRepository
import fr.shikkanime.models.UserRole
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import org.koin.core.annotation.Single

@Suppress("unused")
@Single(binds = [UserRepository::class])
class UserRepositoryImpl : UserRepository() {
    override fun findAllByRoles(vararg roles: UserRole): List<UserEntity> =
        UserEntity.find { UserTable.roles inList listOf(roles.toSet()) }
            .toList()

    override fun findByIdentifier(identifier: ByteArray): UserEntity? =
        UserEntity.find { UserTable.identifier eq identifier }
            .limit(1)
            .firstOrNull()

    override fun findByUsername(username: String): UserEntity? =
        UserEntity.find { UserTable.username eq username }
            .limit(1)
            .firstOrNull()
}