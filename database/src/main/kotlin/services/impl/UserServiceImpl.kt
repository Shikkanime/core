package fr.shikkanime.database.services.impl

import fr.shikkanime.database.entities.UserTable
import fr.shikkanime.database.repositories.UserRepository
import fr.shikkanime.database.services.UserService
import fr.shikkanime.models.User
import fr.shikkanime.models.UserRole
import org.jetbrains.exposed.v1.crypt.hash
import org.koin.core.annotation.Single

/**
 * Returns `true` when the receiver carries no byte.
 *
 * Primitive arrays have no `isNullOrEmpty` overload in the standard library, so the check is
 * spelled out for the external identifier of an account.
 *
 * @return `true` when the receiver is `null` or empty.
 */
private fun ByteArray?.isNullOrEmpty(): Boolean =
    this == null || this.isEmpty()

@Suppress("unused")
@Single(binds = [UserService::class])
internal class UserServiceImpl(private val userRepository: UserRepository) : UserService {
    override fun findByUsername(username: String): User? =
        userRepository.findByUsername(username)

    override fun findAllByRoles(vararg roles: UserRole): List<User> =
        userRepository.findAllByRoles(*roles)

    override fun passwordMatches(user: User, plainPassword: String): Boolean =
        user.id?.let(userRepository::findById)?.password?.matches(plainPassword) == true

    override fun create(
        identifier: ByteArray?,
        username: String?,
        password: String?,
        roles: Set<UserRole>?
    ): User {
        // An account needs at least one sign-in key: identifier or username
        if (identifier.isNullOrEmpty() && username.isNullOrBlank())
            throw IllegalArgumentException("Either identifier or username must be provided")

        // The sign-in keys are unique: an already known account refuses the creation
        identifier?.let(userRepository::findByIdentifier)
            ?.let { throw IllegalArgumentException("User with identifier already exists") }

        username?.let(userRepository::findByUsername)
            ?.let { throw IllegalArgumentException("User with username already exists") }

        return userRepository.save(
            identifier = identifier,
            username = username,
            hashedPassword = password?.let(UserTable.password::hash),
            roles = roles
        )
    }
}
