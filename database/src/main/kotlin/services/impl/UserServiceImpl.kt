package fr.shikkanime.database.services.impl

import fr.shikkanime.database.entities.UserEntity
import fr.shikkanime.database.entities.UserTable
import fr.shikkanime.database.repositories.UserRepository
import fr.shikkanime.database.services.UserService
import fr.shikkanime.models.UserRole
import org.jetbrains.exposed.v1.crypt.hash
import org.koin.core.annotation.Single

@Suppress("unused")
@Single
class UserServiceImpl(private val userRepository: UserRepository) : UserService {
    private fun ByteArray?.isNullOrEmpty(): Boolean =
        this == null || this.isEmpty()

    override fun create(
        identifier: ByteArray?,
        username: String?,
        password: String?,
        roles: Set<UserRole>?
    ): UserEntity {
        if (identifier.isNullOrEmpty() && username.isNullOrBlank()) {
            throw IllegalArgumentException("Either identifier or username must be provided")
        }

        identifier?.let(userRepository::findByIdentifier)
            ?.let { throw IllegalArgumentException("User with identifier already exists") }

        username?.let(userRepository::findByUsername)
            ?.let { throw IllegalArgumentException("User with username already exists") }

        return UserEntity.new {
            this.identifier = identifier
            this.username = username
            this.password = password?.let(UserTable.password::hash)
            this.roles = roles
        }
    }
}