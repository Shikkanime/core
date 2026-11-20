package fr.shikkanime.database.repositories.impl

import fr.shikkanime.database.entities.UserEntity
import fr.shikkanime.database.entities.UserTable
import fr.shikkanime.database.repositories.UserRepository
import fr.shikkanime.models.User
import fr.shikkanime.models.UserRole
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.crypt.Hashed
import org.koin.core.annotation.Single

@Suppress("unused")
@Single(binds = [UserRepository::class])
internal class UserRepositoryImpl : UserRepository() {
    override fun findAllByRoles(vararg roles: UserRole): List<User> =
        UserEntity.all()
            .toList()
            .map(UserEntity::toModel)
            .filter { user -> user.roles?.any(roles::contains) == true }

    override fun findByIdentifier(identifier: ByteArray): User? =
        UserEntity.find { UserTable.identifier eq identifier }
            .limit(1)
            .firstOrNull()
            ?.toModel()

    override fun findByUsername(username: String): User? =
        UserEntity.find { UserTable.username eq username }
            .limit(1)
            .firstOrNull()
            ?.toModel()

    override fun save(
        identifier: ByteArray?,
        username: String?,
        hashedPassword: Hashed?,
        roles: Set<UserRole>?
    ): User =
        UserEntity.new {
            this.identifier = identifier
            this.username = username
            this.password = hashedPassword
            this.roles = roles
        }
            .applyFlush()
            .toModel()
}
